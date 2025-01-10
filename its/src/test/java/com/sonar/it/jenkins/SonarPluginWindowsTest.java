/*
 * Jenkins :: Integration Tests
 * Copyright (C) 2013-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.sonar.it.jenkins;

import com.sonar.it.jenkins.utility.JenkinsUtils.FailedExecutionException;
import com.sonar.it.jenkins.utility.RunOnlyOnWindows;
import java.io.File;
import java.nio.file.Paths;
import org.jenkinsci.test.acceptance.junit.WithPlugins;
import org.jenkinsci.test.acceptance.po.Build;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.sonar.it.jenkins.utility.JenkinsUtils.DEFAULT_SONARQUBE_INSTALLATION;
import static java.util.regex.Matcher.quoteReplacement;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(value= RunOnlyOnWindows.class)
public class SonarPluginWindowsTest extends SonarPluginTestSuite {

  private final File consoleApp1Folder = new File(csharpFolder, "ConsoleApplication1");

  @Test
  @WithPlugins({"workflow-aggregator", "msbuild"})
  public void msbuild_pipeline() {
    MSBuildScannerInstallation.install(jenkins, OLDEST_INSTALLABLE_MSBUILD_VERSION, false);
    jenkinsOrch.configureSonarInstallation(ORCHESTRATOR);

    String script = "withSonarQubeEnv('" + DEFAULT_SONARQUBE_INSTALLATION + "') {\n"
      + "  bat 'xcopy " + Paths.get("projects/csharp").toAbsolutePath().toString().replaceAll("\\\\", quoteReplacement("\\\\")) + " . /s /e /y'\n"
      + "  def sqScannerMsBuildHome = tool 'Scanner for MSBuild " + OLDEST_INSTALLABLE_MSBUILD_VERSION + "'\n"
      + "  bat \"${sqScannerMsBuildHome}\\\\MSBuild.SonarQube.Runner.exe begin /k:csharp /n:CSharp /v:1.0\"\n"
      + "  bat '\\\"%MSBUILD_PATH%\\\" /t:Rebuild'\n"
      + "  bat \"${sqScannerMsBuildHome}\\\\MSBuild.SonarQube.Runner.exe end\"\n"
      + "}";
    assertThat(runAndGetLogs("csharp-pipeline", script)).contains("ANALYSIS SUCCESSFUL, you can browse");
  }

  @Test
  @WithPlugins({"msbuild"})
  public void freestyle_job_with_scanner_for_ms_build_2_3_2() {
    MSBuildScannerInstallation.install(jenkins, "2.3.2.573", false);
    MSBuildScannerInstallation.install(jenkins, OLDEST_INSTALLABLE_MSBUILD_VERSION, false);
    jenkinsOrch.configureSonarInstallation(ORCHESTRATOR)
      .configureMSBuild(ORCHESTRATOR);

    String jobName = "msbuild-sq-runner-2_3_2";
    String projectKey = "msbuild-sq-runner-2_3_2";
    assertThat(getProject(projectKey)).isNull();
    Build result = jenkinsOrch
      .newFreestyleJobWithScannerForMsBuild(jobName, null, jsFolder, projectKey, "JS with space", "1.0", "2.3.2.573", null, false)
      .executeJobQuietly(jobName);

    assertThat(result.getConsole())
      .contains(
        "tools" + File.separator + "hudson.plugins.sonar.MsBuildSQRunnerInstallation" + File.separator + "Scanner_for_MSBuild_2.3.2.573" + File.separator
          + "MSBuild.SonarQube.Runner.exe begin /k:" + projectKey + " \"/n:JS with space\" /v:1.0 /d:sonar.host.url="
          + ORCHESTRATOR.getServer().getUrl());
  }

  @Test
  @WithPlugins({"msbuild"})
  public void freestyle_job_with_scanner_for_ms_build_3_0() {
    MSBuildScannerInstallation.install(jenkins, "2.3.2.573", false);
    MSBuildScannerInstallation.install(jenkins, OLDEST_INSTALLABLE_MSBUILD_VERSION, false);
    jenkinsOrch.configureSonarInstallation(ORCHESTRATOR)
      .configureMSBuild(ORCHESTRATOR);

    String jobName = "msbuild-sq-runner-3_0";
    String projectKey = "msbuild-sq-runner-3_0";
    assertThat(getProject(projectKey)).isNull();
    Build result = jenkinsOrch
      .newFreestyleJobWithScannerForMsBuild(jobName, null, jsFolder, projectKey, "JS with space", "1.0",
        OLDEST_INSTALLABLE_MSBUILD_VERSION, null, false)
      .executeJobQuietly(jobName);

    assertThat(result.getConsole())
      .contains(
        "tools" + File.separator + "hudson.plugins.sonar.MsBuildSQRunnerInstallation" + File.separator + "Scanner_for_MSBuild_3.0.0.629" + File.separator
          + "MSBuild.SonarQube.Runner.exe begin /k:" + projectKey + " \"/n:JS with space\" /v:1.0 /d:sonar.host.url="
          + ORCHESTRATOR.getServer().getUrl());
  }

  @Test
  @WithPlugins({"msbuild"})
  public void freestyle_job_with_scanner_for_ms_build() throws FailedExecutionException {
    MSBuildScannerInstallation.install(jenkins, LATEST_INSTALLABLE_MSBUILD_VERSION, false);
    jenkinsOrch.configureSonarInstallation(ORCHESTRATOR)
      .configureMSBuild(ORCHESTRATOR);

    String jobName = "csharp";
    String projectKey = "csharp";
    assertThat(getProject(projectKey)).isNull();
    jenkinsOrch
      .newFreestyleJobWithScannerForMsBuild(jobName, null, consoleApp1Folder, projectKey, "CSharp", "1.0", LATEST_INSTALLABLE_MSBUILD_VERSION, "ConsoleApplication1.sln", false)
      .executeJob(jobName);

    waitForComputationOnSQServer();
    assertThat(getProject(projectKey)).isNotNull();
    assertSonarUrlOnJob(jobName, projectKey);
  }

}
