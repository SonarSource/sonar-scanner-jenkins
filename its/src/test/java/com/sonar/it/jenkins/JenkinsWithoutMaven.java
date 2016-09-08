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
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.locator.URLLocation;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.PropertyUpdateQuery;
import org.sonar.wsclient.services.ResourceQuery;

import static org.fest.assertions.Assertions.assertThat;

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
      .installPlugin(URLLocation.create(new URL("http://mirrors.jenkins-ci.org/plugins/jquery/1.11.2-0/jquery.hpi")))
      .installPlugin(URLLocation.create(new URL("http://mirrors.jenkins-ci.org/plugins/filesystem_scm/1.20/filesystem_scm.hpi")))
      .installPlugin(sqJenkinsPluginLocation)
      .configureSQScannerInstallation("2.4", 0)
      .configureSQScannerInstallation("2.6.1", 1)
      .configureMsBuildSQScanner_installation("1.1", 0)
      .configureMsBuildSQScanner_installation("2.0", 1)
      .configureSonarInstallation(orchestrator);
    jenkins.checkSavedSonarInstallation(orchestrator);
    jenkins.configureDefaultQG(orchestrator);
  }

  @Before
  public void resetData() throws Exception {
    orchestrator.resetData();
  }

  @Test
  public void testFreestyleJobWithSonarQubeScanner_use_sq_scanner_2_4() throws Exception {
    String jobName = "abacus-runner-sq-2.4";
    String projectKey = "abacus-runner-2.4";
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(projectKey))).isNull();
    BuildResult result = jenkins
      .newFreestyleJobWithSQScanner(jobName, "-v", new File("projects", "abacus"), "2.4",
        "sonar.projectKey", projectKey,
        "sonar.projectVersion", "1.0",
        "sonar.projectName", "Abacus",
        "sonar.sources", "src/main/java")
      .executeJob(jobName);

    if (JenkinsTestSuite.isWindows()) {
      assertThat(result.getLogs()).contains("sonar-runner.bat");
    } else {
      assertThat(result.getLogs()).contains("sonar-runner");
    }
    assertThat(result.getLogs()).contains("SonarQube Runner 2.4");
  }

  @Test
  public void testFreestyleJobWithSonarQubeScanner_use_sq_scanner_2_6_1() throws Exception {
    String jobName = "abacus-runner-sq-2.6.1";
    String projectKey = "abacus-runner-2.6.1";
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(projectKey))).isNull();
    BuildResult result = jenkins
      .newFreestyleJobWithSQScanner(jobName, "-v", new File("projects", "abacus"), "2.6.1",
        "sonar.projectKey", projectKey,
        "sonar.projectVersion", "1.0",
        "sonar.projectName", "Abacus",
        "sonar.sources", "src/main/java")
      .executeJob(jobName);

    if (JenkinsTestSuite.isWindows()) {
      assertThat(result.getLogs()).contains("sonar-scanner.bat");
    } else {
      assertThat(result.getLogs()).contains("sonar-scanner");
    }
    assertThat(result.getLogs()).contains("SonarQube Scanner 2.6.1");
  }

  @Test
  public void testFreestyleJobWithMsBuildSQRunner_2_0() {
    File toolPath = new File(jenkins.getServer().getHome().getAbsolutePath() + File.separator + "tools" + File.separator + "hudson.plugins.sonar.MsBuildSQRunnerInstallation");
    String jobName = "abacus-msbuild-sq-runner-2-0";
    String projectKey = "abacus-msbuild-sq-runner-2-0";
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(projectKey))).isNull();
    BuildResult result = jenkins
      .newFreestyleJobWithMsBuildSQRunner(jobName, null, new File("projects", "abacus"), projectKey, "Abacus with space", "1.0", "2.0")
      .executeJobQuietly(jobName);

    assertThat(result.getLogs())
      .contains(
        "tools" + File.separator + "hudson.plugins.sonar.MsBuildSQRunnerInstallation" + File.separator + "Scanner_for_MSBuild_2.0" + File.separator
          + "MSBuild.SonarQube.Runner.exe begin /k:" + projectKey + " \"/n:Abacus with space\" /v:1.0 /d:sonar.host.url="
          + orchestrator.getServer().getUrl());

    assertThat(toolPath).isDirectory();
  }

  @Test
  public void testFreestyleJobWithMsBuildSQRunner_1_1() {
    File toolPath = new File(jenkins.getServer().getHome().getAbsolutePath() + File.separator + "tools" + File.separator + "hudson.plugins.sonar.MsBuildSQRunnerInstallation");
    String jobName = "abacus-msbuild-sq-runner-1-1";
    String projectKey = "abacus-msbuild-sq-runner-1-1";
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(projectKey))).isNull();
    BuildResult result = jenkins
      .newFreestyleJobWithMsBuildSQRunner(jobName, null, new File("projects", "abacus"), projectKey, "Abacus with space", "1.0", "1.1")
      .executeJobQuietly(jobName);

    assertThat(result.getLogs())
      .contains(
        "tools" + File.separator + "hudson.plugins.sonar.MsBuildSQRunnerInstallation" + File.separator + "Scanner_for_MSBuild_1.1" + File.separator
          + "MSBuild.SonarQube.Runner.exe begin /k:" + projectKey + " \"/n:Abacus with space\" /v:1.0 /d:sonar.host.url="
          + orchestrator.getServer().getUrl());

    assertThat(toolPath).isDirectory();
  }

  @Test
  public void testNoSonarPublisher() {
    String jobName = "no Sonar Publisher";
    jenkins
      .assertNoSonarPublisher(jobName, new File("projects", "noPublisher"));
  }

}
