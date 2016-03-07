/*
 * Jenkins :: Integration Tests
 * Copyright (C) 2013 ${owner}
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package com.sonar.it.jenkins;

import com.sonar.it.jenkins.orchestrator.JenkinsOrchestrator;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SynchronousAnalyzer;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.locator.URLLocation;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.PropertyUpdateQuery;
import org.sonar.wsclient.services.ResourceQuery;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

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
      .disablePlugin("maven-plugin")
      .installPlugin(URLLocation.create(new URL("http://mirrors.jenkins-ci.org/plugins/jquery/1.11.2-0/jquery.hpi")))
      .installPlugin(URLLocation.create(new URL("http://mirrors.jenkins-ci.org/plugins/filesystem_scm/1.20/filesystem_scm.hpi")))
      .installPlugin(sqJenkinsPluginLocation)
      .configureMavenInstallation()
      .configureSonarRunner2_4Installation()
      .configureMsBuildSQScanner_installation()
      .configureSonarInstallation(orchestrator);
    jenkins.checkSavedSonarInstallation(orchestrator);
    jenkins.configureDefaultQG(orchestrator);
  }

  @Before
  public void resetData() throws Exception {
    orchestrator.resetData();
  }

  @Test
  public void testFreestyleJobWithSonarRunner() throws Exception {
    String jobName = "abacus-runner";
    String projectKey = "abacus-runner";
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(projectKey))).isNull();
    jenkins
      .newFreestyleJobWithSonarRunner(jobName, "-X", new File("projects", "abacus"),
        "sonar.projectKey", projectKey,
        "sonar.projectVersion", "1.0",
        "sonar.projectName", "Abacus",
        "sonar.sources", "src/main/java")
      .executeJob(jobName);
    waitForComputationOnSQServer();
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(projectKey))).isNotNull();
    assertSonarUrlOnJob(jobName, projectKey);
    jenkins.assertQGOnProjectPage(jobName);
  }
  
  private void assertSonarUrlOnJob(String jobName, String projectKey) {
    assertThat(jenkins.getSonarUrlOnJob(jobName)).startsWith(orchestrator.getServer().getUrl());
    assertThat(jenkins.getSonarUrlOnJob(jobName)).endsWith(projectKey);
  }

  private void waitForComputationOnSQServer() {
    new SynchronousAnalyzer(orchestrator.getServer()).waitForDone();
  }
}
