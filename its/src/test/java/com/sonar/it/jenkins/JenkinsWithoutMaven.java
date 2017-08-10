/*
 * Jenkins :: Integration Tests
 * Copyright (C) 2013-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.sonar.it.jenkins.orchestrator.JenkinsOrchestrator;
import com.sonar.it.jenkins.orchestrator.JenkinsOrchestrator.FailedExecutionException;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SynchronousAnalyzer;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Location;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.PropertyUpdateQuery;

import static com.sonar.it.jenkins.JenkinsTestSuite.getProject;
import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

public class JenkinsWithoutMaven {
  @ClassRule
  public static Orchestrator orchestrator = JenkinsTestSuite.ORCHESTRATOR;

  @ClassRule
  public static JenkinsOrchestrator jenkins = JenkinsOrchestrator.builderEnv().build();

  @BeforeClass
  public static void setUpSonar() throws MalformedURLException {
    // Workaround for SONAR-4257
    orchestrator.getServer().getAdminWsClient().update(new PropertyUpdateQuery("sonar.core.serverBaseURL", orchestrator.getServer().getUrl()));
  }

  @BeforeClass
  public static void setUpJenkins() throws IOException {
    orchestrator.resetData();
    Location sqJenkinsPluginLocation = FileLocation.of("../target/sonar.hpi");
    jenkins
      .installPlugin("filesystem_scm")
      .installPlugin("jquery")
      .installPlugin("msbuild")
      .installPlugin(sqJenkinsPluginLocation)
      .configureSQScannerInstallation("2.4", 0)
      .configureSQScannerInstallation("2.8", 1)
      .configureMsBuildSQScanner_installation("2.3.2.573", 0)
      .configureMsBuildSQScanner_installation("3.0.0.629", 1)
      .configureSonarInstallation(orchestrator);
    if (SystemUtils.IS_OS_WINDOWS) {
      jenkins.configureMSBuildInstallation();
    }
    jenkins.checkSavedSonarInstallation(orchestrator);
    jenkins.configureDefaultQG(orchestrator);
  }

  @Before
  public void resetData() throws Exception {
    orchestrator.resetData();
  }

  @Test
  public void testFreestyleJobWithSonarQubeScanner_use_sq_scanner_2_4() throws Exception {
    String jobName = "js-runner-sq-2.4";
    String projectKey = "js-runner-2.4";
    assertThat(getProject(projectKey)).isNull();
    BuildResult result = jenkins
      .newFreestyleJobWithSQScanner(jobName, "-v", new File("projects", "js"), "2.4",
        "sonar.projectKey", projectKey,
        "sonar.projectVersion", "1.0",
        "sonar.projectName", "Abacus",
        "sonar.sources", "src")
      .executeJob(jobName);

    if (JenkinsTestSuite.isWindows()) {
      assertThat(result.getLogs()).contains("sonar-runner.bat");
    } else {
      assertThat(result.getLogs()).contains("sonar-runner");
    }
    assertThat(result.getLogs()).contains("SonarQube Runner 2.4");
  }

  @Test
  public void testFreestyleJobWithSonarQubeScanner_use_sq_scanner_2_8() throws Exception {
    String jobName = "js-runner-sq-2.8";
    String projectKey = "js-runner-2.8";
    assertThat(getProject(projectKey)).isNull();
    BuildResult result = jenkins
      .newFreestyleJobWithSQScanner(jobName, "-v", new File("projects", "js"), "2.8",
        "sonar.projectKey", projectKey,
        "sonar.projectVersion", "1.0",
        "sonar.projectName", "Abacus",
        "sonar.sources", "src")
      .executeJob(jobName);

    if (JenkinsTestSuite.isWindows()) {
      assertThat(result.getLogs()).contains("sonar-scanner.bat");
    } else {
      assertThat(result.getLogs()).contains("sonar-scanner");
    }
    assertThat(result.getLogs()).contains("SonarQube Scanner 2.8");
  }

  @Test
  public void testFreestyleJobWithScannerForMsBuild() throws FailedExecutionException {
    assumeTrue(SystemUtils.IS_OS_WINDOWS);
    String jobName = "csharp";
    String projectKey = "csharp";
    assertThat(getProject(projectKey)).isNull();
    jenkins
      .newFreestyleJobWithScannerForMsBuild(jobName, null, new File("projects", "csharp"), projectKey, "CSharp", "1.0", "3.0.0.629", "ConsoleApplication1.sln")
      .executeJob(jobName);

    waitForComputationOnSQServer();
    assertThat(getProject(projectKey)).isNotNull();
    assertSonarUrlOnJob(jobName, projectKey);
  }

  @Test
  public void testFreestyleJobWithScannerForMsBuild_3_0() {
    assumeTrue(SystemUtils.IS_OS_WINDOWS);
    File toolPath = new File(jenkins.getServer().getHome().getAbsolutePath() + File.separator + "tools" + File.separator + "hudson.plugins.sonar.MsBuildSQRunnerInstallation");
    String jobName = "msbuild-sq-runner-3_0";
    String projectKey = "msbuild-sq-runner-3_0";
    assertThat(getProject(projectKey)).isNull();
    BuildResult result = jenkins
      .newFreestyleJobWithScannerForMsBuild(jobName, null, new File("projects", "js"), projectKey, "JS with space", "1.0", "3.0.0.629", null)
      .executeJobQuietly(jobName);

    assertThat(result.getLogs())
      .contains(
        "tools" + File.separator + "hudson.plugins.sonar.MsBuildSQRunnerInstallation" + File.separator + "Scanner_for_MSBuild_3.0.0.629" + File.separator
          + "MSBuild.SonarQube.Runner.exe begin /k:" + projectKey + " \"/n:JS with space\" /v:1.0 /d:sonar.host.url="
          + orchestrator.getServer().getUrl());

    assertThat(toolPath).isDirectory();
  }

  @Test
  public void testFreestyleJobWithScannerForMsBuild_2_3_2() {
    assumeTrue(SystemUtils.IS_OS_WINDOWS);
    File toolPath = new File(jenkins.getServer().getHome().getAbsolutePath() + File.separator + "tools" + File.separator + "hudson.plugins.sonar.MsBuildSQRunnerInstallation");
    String jobName = "msbuild-sq-runner-2_3_2";
    String projectKey = "msbuild-sq-runner-2_3_2";
    assertThat(getProject(projectKey)).isNull();
    BuildResult result = jenkins
      .newFreestyleJobWithScannerForMsBuild(jobName, null, new File("projects", "js"), projectKey, "JS with space", "1.0", "2.3.2.573", null)
      .executeJobQuietly(jobName);

    assertThat(result.getLogs())
      .contains(
        "tools" + File.separator + "hudson.plugins.sonar.MsBuildSQRunnerInstallation" + File.separator + "Scanner_for_MSBuild_2.3.2.573" + File.separator
          + "MSBuild.SonarQube.Runner.exe begin /k:" + projectKey + " \"/n:JS with space\" /v:1.0 /d:sonar.host.url="
          + orchestrator.getServer().getUrl());

    assertThat(toolPath).isDirectory();
  }

  @Test
  public void testNoSonarPublisher() {
    // Maven plugin no more installed by default in version 2
    assumeTrue(jenkins.getServer().getVersion().isGreaterThanOrEquals("2"));
    String jobName = "no Sonar Publisher";
    jenkins.assertNoSonarPublisher(jobName, new File("projects", "noPublisher"));
  }

  private void assertSonarUrlOnJob(String jobName, String projectKey) {
    assertThat(jenkins.getSonarUrlOnJob(jobName)).startsWith(orchestrator.getServer().getUrl());
    assertThat(jenkins.getSonarUrlOnJob(jobName)).endsWith(projectKey);
  }

  private static void waitForComputationOnSQServer() {
    new SynchronousAnalyzer(orchestrator.getServer()).waitForDone();
  }

}
