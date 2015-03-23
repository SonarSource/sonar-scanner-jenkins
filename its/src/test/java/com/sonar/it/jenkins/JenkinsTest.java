/*
 * Copyright (C) 2013-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.jenkins;

import com.sonar.it.jenkins.orchestrator.JenkinsOrchestrator;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SynchronousAnalyzer;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.orchestrator.locator.URLLocation;
import com.sonar.orchestrator.version.Version;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.PropertyUpdateQuery;
import org.sonar.wsclient.services.ResourceQuery;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import static org.fest.assertions.Assertions.assertThat;

public class JenkinsTest {

  @ClassRule
  public static Orchestrator orchestrator = JenkinsTestSuite.ORCHESTRATOR;

  @ClassRule
  public static JenkinsOrchestrator jenkins = JenkinsOrchestrator.builderEnv().build();

  private static Version sonarPluginVersion;

  @BeforeClass
  public static void setUpSonar() throws MalformedURLException {
    // Workaround for SONAR-4257
    orchestrator.getServer().getAdminWsClient().update(new PropertyUpdateQuery("sonar.core.serverBaseURL", orchestrator.getServer().getUrl()));
  }

  @BeforeClass
  public static void setUpJenkins() throws MalformedURLException {
    sonarPluginVersion = orchestrator.getConfiguration().getPluginVersion("sonarJenkins");
    Location pluginLocation;
    if (sonarPluginVersion.isSnapshot()) {
      // For SNAPSHOT look in Nexus
      pluginLocation = MavenLocation.builder()
        .setKey("org.jenkins-ci.plugins", "sonar", sonarPluginVersion.toString())
        .withPackaging("hpi")
        .build();
    }
    else {
      // For release look in Jenkins update center
      pluginLocation = URLLocation.create(new URL("http://mirrors.jenkins-ci.org/plugins/sonar/" + sonarPluginVersion + "/sonar.hpi"));
    }
    jenkins
      .setSonarPluginVersion(sonarPluginVersion)
      .installPlugin(URLLocation.create(new URL("http://mirrors.jenkins-ci.org/plugins/filesystem_scm/1.20/filesystem_scm.hpi")))
      .installPlugin(pluginLocation)
      .configureMavenInstallation()
      .configureSonarRunner2_4Installation()
      .configureSonarInstallation(orchestrator);
  }

  @Before
  public void resetData() throws Exception {
    orchestrator.resetData();
  }

  @Test
  public void testMavenJob() throws Exception {
    String jobName = "abacus-maven";
    String projectKey = "org.codehaus.sonar-plugins:sonar-abacus-plugin";
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(projectKey))).isNull();
    jenkins
      .newMavenJobWithSonar(jobName, new File("projects", "abacus"), null)
      .triggerBuild(jobName);
    waitForComputationOnSQServer();
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(projectKey))).isNotNull();
    assertSonarUrlOnJob(jobName, projectKey);
  }

  @Test
  public void testMavenJobWithBranch() throws Exception {
    String jobName = "abacus-maven-branch";
    String projectKey = "org.codehaus.sonar-plugins:sonar-abacus-plugin:branch";
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(projectKey))).isNull();
    jenkins
      .newMavenJobWithSonar(jobName, new File("projects", "abacus"), "branch")
      .triggerBuild(jobName);
    waitForComputationOnSQServer();
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(projectKey))).isNotNull();
    assertSonarUrlOnJob(jobName, projectKey);
  }

  @Test
  public void testFreestyleJobWithSonarMaven() throws Exception {
    String jobName = "abacus-freestyle";
    String projectKey = "org.codehaus.sonar-plugins:sonar-abacus-plugin";
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(projectKey))).isNull();
    jenkins
      .newFreestyleJobWithSonar(jobName, new File("projects", "abacus"), null)
      .triggerBuild(jobName);
    waitForComputationOnSQServer();
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(projectKey))).isNotNull();
    assertSonarUrlOnJob(jobName, projectKey);
  }

  @Test
  public void testFreestyleJobWithSonarMavenAndBranch() throws Exception {
    String jobName = "abacus-freestyle-branch";
    String projectKey = "org.codehaus.sonar-plugins:sonar-abacus-plugin:branch";
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(projectKey))).isNull();
    jenkins
      .newFreestyleJobWithSonar(jobName, new File("projects", "abacus"), "branch")
      .triggerBuild(jobName);
    waitForComputationOnSQServer();
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(projectKey))).isNotNull();
    assertSonarUrlOnJob(jobName, projectKey);
  }

  @Test
  public void testFreestyleJobWithSonarRunner() throws Exception {
    String jobName = "abacus-runner";
    String projectKey = "abacus-runner";
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(projectKey))).isNull();
    jenkins
      .newFreestyleJobWithSonarRunner(jobName, new File("projects", "abacus"),
        "sonar.projectKey", projectKey,
        "sonar.projectVersion", "1.0",
        "sonar.projectName", "Abacus",
        "sonar.sources", "src/main/java")
      .triggerBuild(jobName);
    waitForComputationOnSQServer();
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(projectKey))).isNotNull();
    assertSonarUrlOnJob(jobName, projectKey);
  }

  @Test
  public void testFreestyleJobWithSonarRunnerAndBranch() throws Exception {
    String jobName = "abacus-runner-branch";
    String projectKey = "abacus-runner:branch";
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(projectKey))).isNull();
    jenkins
      .newFreestyleJobWithSonarRunner(jobName, new File("projects", "abacus"),
        "sonar.projectKey", "abacus-runner",
        "sonar.projectVersion", "1.0",
        "sonar.projectName", "Abacus",
        "sonar.sources", "src/main/java",
        "sonar.branch", "branch")
      .triggerBuild(jobName);
    waitForComputationOnSQServer();
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(projectKey))).isNotNull();
    assertSonarUrlOnJob(jobName, projectKey);
  }

  private void assertSonarUrlOnJob(String jobName, String projectKey) {
    // Computation of Sonar URL was not reliable before 2.1 & Sonar 3.6
    if (sonarPluginVersion.isGreaterThanOrEquals("2.1")) {
      assertThat(jenkins.getSonarUrlOnJob(jobName)).startsWith(orchestrator.getServer().getUrl());
      if (orchestrator.getServer().version().isGreaterThanOrEquals("3.6")) {
        assertThat(jenkins.getSonarUrlOnJob(jobName)).endsWith(projectKey);
      }
    }
  }

  private void waitForComputationOnSQServer() {
    new SynchronousAnalyzer(orchestrator.getServer()).waitForDone();
  }

}
