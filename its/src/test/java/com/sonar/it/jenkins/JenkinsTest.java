/*
 * Jenkins :: Integration Tests
 * Copyright (C) 2013-2018 SonarSource SA
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

import com.sonar.it.jenkins.orchestrator.JenkinsOrchestrator;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SynchronousAnalyzer;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Location;
import java.io.File;
import java.net.URLEncoder;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.PropertyUpdateQuery;

import static com.sonar.it.jenkins.JenkinsTestSuite.getProject;
import static org.fest.assertions.Assertions.assertThat;

public class JenkinsTest {

  // Jenkins should only talk to the URL actually configured in a SonarInstallation
  final static String SONAR_PUBLIC_BASE_URL = "http://sonar_public_domain/";

  @ClassRule
  public static Orchestrator orchestrator = JenkinsTestSuite.ORCHESTRATOR;

  @ClassRule
  public static JenkinsOrchestrator jenkins = JenkinsOrchestrator.builderEnv().build();

  @BeforeClass
  public static void setUpSonar() {
    // Workaround for SONAR-4257
    orchestrator.getServer().getAdminWsClient().update(new PropertyUpdateQuery("sonar.core.serverBaseURL", SONAR_PUBLIC_BASE_URL));
  }

  @BeforeClass
  public static void setUpJenkins() {
    if (jenkins.getServer().getVersion().isGreaterThanOrEquals(2, 0)) {
      // Maven plugin no more installed by default
      jenkins.installPlugin("maven-plugin");
    }

    Location sqJenkinsPluginLocation = FileLocation.of("../target/sonar.hpi");
    jenkins
      .installPlugin("filesystem_scm")
      .installPlugin("jquery")
      .installPlugin(sqJenkinsPluginLocation)
      .configureMavenInstallation()
      // Single installation
      .configureSQScannerInstallation("2.8", 0)
      .configureMsBuildSQScanner_installation("3.0.0.629", false,0)
      .configureSonarInstallation(orchestrator);
    jenkins.checkSavedSonarInstallation(orchestrator);
    jenkins.configureDefaultQG(orchestrator);
  }

  @Before
  public void resetData() {
    orchestrator.resetData();
  }

  @Test
  public void testMavenJob() {
    String jobName = "abacus-maven";
    String projectKey = "org.codehaus.sonar-plugins:sonar-abacus-plugin";
    assertThat(getProject(projectKey)).isNull();
    jenkins
      .newMavenJobWithSonar(jobName, new File("projects", "abacus"), null)
      .executeJob(jobName);
    waitForComputationOnSQServer();
    assertThat(getProject(projectKey)).isNotNull();
    assertSonarUrlOnJob(jobName, projectKey);
    jenkins.assertQGOnProjectPage(jobName);
  }

  @Test
  public void testMavenJobWithBranch() {
    String jobName = "abacus-maven-branch";
    String projectKey = "org.codehaus.sonar-plugins:sonar-abacus-plugin:branch";
    assertThat(getProject(projectKey)).isNull();
    jenkins
      .newMavenJobWithSonar(jobName, new File("projects", "abacus"), "branch")
      .executeJob(jobName);
    waitForComputationOnSQServer();
    assertThat(getProject(projectKey)).isNotNull();
    assertSonarUrlOnJob(jobName, projectKey);
    jenkins.assertQGOnProjectPage(jobName);
  }

  @Test
  public void testVariableInjection() throws JenkinsOrchestrator.FailedExecutionException {
    String jobName = "abacus-freestyle-vars";
    String projectKey = "org.codehaus.sonar-plugins:sonar-abacus-plugin";
    assertThat(getProject(projectKey)).isNull();

    jenkins.enableInjectionVars(true)
      .newFreestyleJobWithMaven(jobName, new File("projects", "abacus"), null, orchestrator)
      .executeJob(jobName);
    waitForComputationOnSQServer();
    assertThat(getProject(projectKey)).isNotNull();
    assertSonarUrlOnJob(jobName, projectKey);
    jenkins.assertQGOnProjectPage(jobName);
  }

  @Test
  public void testFreestyleJobWithSonarMaven() {
    String jobName = "abacus-freestyle";
    String projectKey = "org.codehaus.sonar-plugins:sonar-abacus-plugin";
    assertThat(getProject(projectKey)).isNull();
    jenkins
      .newFreestyleJobWithSonar(jobName, new File("projects", "abacus"), null)
      .executeJob(jobName);
    waitForComputationOnSQServer();
    assertThat(getProject(projectKey)).isNotNull();
    assertSonarUrlOnJob(jobName, projectKey);
    jenkins.assertQGOnProjectPage(jobName);
  }

  @Test
  public void testFreestyleJobWithSonarMavenAndBranch() {
    String jobName = "abacus-freestyle-branch";
    String projectKey = "org.codehaus.sonar-plugins:sonar-abacus-plugin:branch";
    assertThat(getProject(projectKey)).isNull();
    jenkins
      .newFreestyleJobWithSonar(jobName, new File("projects", "abacus"), "branch")
      .executeJob(jobName);
    waitForComputationOnSQServer();
    assertThat(getProject(projectKey)).isNotNull();
    assertSonarUrlOnJob(jobName, projectKey);
    jenkins.assertQGOnProjectPage(jobName);
  }

  @Test
  public void testFreestyleJobWithSonarQubeScanner() {
    String jobName = "js-runner";
    String projectKey = "js-runner";
    assertThat(getProject(projectKey)).isNull();
    jenkins
      .newFreestyleJobWithSQScanner(jobName, "-X", new File("projects", "js"), null,
        "sonar.projectKey", projectKey,
        "sonar.projectVersion", "1.0",
        "sonar.projectName", "Abacus",
        "sonar.sources", "src")
      .executeJob(jobName);
    waitForComputationOnSQServer();
    assertThat(getProject(projectKey)).isNotNull();
    assertSonarUrlOnJob(jobName, projectKey);
    jenkins.assertQGOnProjectPage(jobName);
  }

  @Test
  public void testFreestyleJobWithSonarQubeScannerAndBranch() {
    String jobName = "js-runner-branch";
    String projectKey = "js-runner:branch";
    assertThat(getProject(projectKey)).isNull();
    BuildResult result = jenkins
      .newFreestyleJobWithSQScanner(jobName, "-X -Duseless=Y -e", new File("projects", "js"), null,
        "sonar.projectKey", "js-runner",
        "sonar.projectVersion", "1.0",
        "sonar.projectName", "Abacus",
        "sonar.sources", "src",
        "sonar.branch", "branch")
      .executeJob(jobName);
    waitForComputationOnSQServer();
    assertThat(getProject(projectKey)).isNotNull();
    assertSonarUrlOnJob(jobName, projectKey);
    jenkins.assertQGOnProjectPage(jobName);
    if (JenkinsTestSuite.isWindows()) {
      assertThat(result.getLogs()).contains("sonar-scanner.bat -X -Duseless=Y -e");
    } else {
      assertThat(result.getLogs()).contains("sonar-scanner -X -Duseless=Y -e");
    }
  }

  private void assertSonarUrlOnJob(String jobName, String projectKey) {
    if (jenkins.getServer().getVersion().isGreaterThanOrEquals(2, 0)) {
      assertThat(jenkins.getSonarUrlOnJob(jobName)).endsWith(URLEncoder.encode(projectKey));
    } else {
      assertThat(jenkins.getSonarUrlOnJob(jobName)).endsWith(projectKey);
    }
    assertThat(jenkins.getSonarUrlOnJob(jobName)).startsWith(SONAR_PUBLIC_BASE_URL);
  }

  private static void waitForComputationOnSQServer() {
    new SynchronousAnalyzer(orchestrator.getServer()).waitForDone();
  }

}
