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
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Location;
import hudson.cli.CLI;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Arrays;
import org.apache.commons.io.output.NullOutputStream;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static com.sonar.it.jenkins.orchestrator.JenkinsOrchestrator.DEFAULT_SONAR_QUBE_INSTALLATION;
import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

public class JenkinsPipelineTest {
  private static final String DUMP_ENV_VARS_PIPELINE_CMD = JenkinsTestSuite.isWindows() ? "bat 'set'" : "sh 'env | sort'";

  @ClassRule
  public static Orchestrator orchestrator = JenkinsTestSuite.ORCHESTRATOR;

  @ClassRule
  public static JenkinsOrchestrator jenkins = JenkinsOrchestrator.builderEnv().build();

  private static CLI cli;

  @BeforeClass
  public static void setUpJenkins() throws MalformedURLException {
    assumeTrue(jenkins.getServer().getVersion().isGreaterThanOrEquals("2"));
    Location sqJenkinsPluginLocation = FileLocation.of("../target/sonar.hpi");
    jenkins
      .installPlugin("jquery")
      // the pipeline plugin -> downloads ~28 other plugins...
      .installPlugin("workflow-aggregator")
      .installPlugin(sqJenkinsPluginLocation)
      .configureSQScannerInstallation("2.6.1", 0)
      .configureSonarInstallation(orchestrator);
    cli = jenkins.getCli();
  }

  @Test
  public void no_sq_vars_without_env_wrapper() throws JenkinsOrchestrator.FailedExecutionException {
    String logs = runAndGetLogs("no-withSonarQubeEnv", DUMP_ENV_VARS_PIPELINE_CMD);
    try {
      verifyEnvVarsExist(logs);
    } catch (AssertionError e) {
      return;
    }
    fail("SonarQube env variables should not exist without withSonarQubeEnv wrapper");
  }

  @Test
  public void env_wrapper_without_params_should_inject_sq_vars() {
    String script = "withSonarQubeEnv { " + DUMP_ENV_VARS_PIPELINE_CMD + " }";
    runAndVerifyEnvVarsExist("withSonarQubeEnv-parameterless", script);
  }

  @Test
  public void env_wrapper_with_specific_sq_should_inject_sq_vars() {
    String script = "withSonarQubeEnv('" + DEFAULT_SONAR_QUBE_INSTALLATION + "') { " + DUMP_ENV_VARS_PIPELINE_CMD + " }";
    runAndVerifyEnvVarsExist("withSonarQubeEnv-SonarQube", script);
  }

  @Test(expected = JenkinsOrchestrator.FailedExecutionException.class)
  public void env_wrapper_with_nonexistent_sq_should_fail() {
    String script = "withSonarQubeEnv('nonexistent') { " + DUMP_ENV_VARS_PIPELINE_CMD + " }";
    runAndVerifyEnvVarsExist("withSonarQubeEnv-nonexistent", script);
  }

  private void runAndVerifyEnvVarsExist(String jobName, String script) {
    String logs = runAndGetLogs(jobName, script);
    verifyEnvVarsExist(logs);
  }

  private String runAndGetLogs(String jobName, String script) {
    createPipelineJobFromScript(jobName, script);
    return jenkins.executeJob(jobName).getLogs();
  }

  private void verifyEnvVarsExist(String logs) {
    assertThat(logs).contains("SONAR_AUTH_TOKEN=");
    assertThat(logs).contains("SONAR_CONFIG_NAME=" + DEFAULT_SONAR_QUBE_INSTALLATION);
    assertThat(logs).contains("SONAR_HOST_URL=");
    assertThat(logs).contains("SONAR_MAVEN_GOAL=sonar:sonar");
    assertThat(logs).contains("SONARQUBE_SCANNER_PARAMS={ \"sonar.host.url\" : \"http:\\/\\/localhost:");
  }

  private static void createPipelineJobFromScript(String jobName, String script) {
    String config = "<?xml version='1.0' encoding='UTF-8'?>\n"
      + "<flow-definition plugin=\"workflow-job@2.6\">\n"
      + "  <actions/>\n"
      + "  <description></description>\n"
      + "  <keepDependencies>false</keepDependencies>\n"
      + "  <properties>\n"
      + "    <jenkins.model.BuildDiscarderProperty>\n"
      + "      <strategy class=\"hudson.tasks.LogRotator\">\n"
      + "        <daysToKeep>-1</daysToKeep>\n"
      + "        <numToKeep>20</numToKeep>\n"
      + "        <artifactDaysToKeep>-1</artifactDaysToKeep>\n"
      + "        <artifactNumToKeep>-1</artifactNumToKeep>\n"
      + "      </strategy>\n"
      + "    </jenkins.model.BuildDiscarderProperty>\n"
      + "    <org.jenkinsci.plugins.workflow.job.properties.PipelineTriggersJobProperty>\n"
      + "      <triggers/>\n"
      + "    </org.jenkinsci.plugins.workflow.job.properties.PipelineTriggersJobProperty>\n"
      + "  </properties>\n"
      + "  <definition class=\"org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition\" plugin=\"workflow-cps@2.13\">\n"
      + "    <script>node { " + script + " }</script>\n"
      + "    <sandbox>true</sandbox>\n"
      + "  </definition>\n"
      + "  <triggers/>\n"
      + "</flow-definition>\n";

    InputStream stdin = new ByteArrayInputStream(config.getBytes());

    NullOutputStream nullOutputStream = new NullOutputStream();
    cli.execute(Arrays.asList("create-job", jobName), stdin, nullOutputStream, nullOutputStream);
  }
}
