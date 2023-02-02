/*
 * Jenkins :: Integration Tests
 * Copyright (C) 2013-2023 SonarSource SA
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

import com.sonar.it.jenkins.utility.JenkinsUtils;
import com.sonar.it.jenkins.utility.SonarQubeScriptBuilder;
import java.io.File;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jenkinsci.test.acceptance.junit.WithPlugins;
import org.jenkinsci.test.acceptance.po.Build;
import org.junit.Test;
import org.sonarqube.ws.Qualitygates;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.qualitygates.CreateConditionRequest;

import static com.google.common.base.Preconditions.checkState;
import static com.sonar.it.jenkins.utility.JenkinsUtils.DEFAULT_SONARQUBE_INSTALLATION;
import static com.sonar.it.jenkins.utility.JenkinsUtils.FailedExecutionException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class SonarPluginTest extends SonarPluginTestSuite {

  private static final String DUMP_ENV_VARS_PIPELINE_CMD = SystemUtils.IS_OS_WINDOWS ? "bat 'set'" : "sh 'env | sort'";
  private static final String SONARQUBE_SCANNER_VERSION = "3.3.0.1492";
  private static final String MVN_PROJECT_KEY = "org.codehaus.sonar-plugins:sonar-abacus-plugin";
  private final File consoleNetCoreFolder = new File(csharpFolder, "NetCoreConsoleApp");

  @Test
  public void scan_project_sonarqube_scanner_v_3_3() {
    SonarScannerInstallation.install(jenkins, SONARQUBE_SCANNER_VERSION);
    jenkinsOrch.configureSonarInstallation(ORCHESTRATOR);

    String jobName = "js-runner-sq-3.3";
    String projectKey = "js-runner-3.3";
    assertThat(getProject(projectKey)).isNull();
    jenkinsOrch
      .newFreestyleJobConfig(jobName, jsFolder)
      .addSonarScannerBuildStep("-Duseless=Y", null,
        "sonar.projectKey", projectKey,
        "sonar.projectVersion", "1.0",
        "sonar.projectName", "Abacus",
        "sonar.sources", "src")
      .save();

    Build result = jenkinsOrch.executeJob(jobName);
    if (SystemUtils.IS_OS_WINDOWS) {
      assertThat(result.getConsole()).contains("sonar-scanner.bat -Duseless=Y");
    } else {
      assertThat(result.getConsole()).contains("sonar-scanner -Duseless=Y");
    }
    assertThat(result.getConsole()).contains("SonarQube Scanner " + SONARQUBE_SCANNER_VERSION);

    waitForComputationOnSQServer();
    assertThat(getProject(projectKey)).isNotNull();
    assertSonarUrlOnJob(jobName, projectKey);
    jenkinsOrch.assertPassedQGOnProjectPage(jobName);
  }

  @Test
  public void scan_project_ms_build_net_core_scanner() {
    checkState(!OLDEST_INSTALLABLE_MSBUILD_VERSION.equals(LATEST_INSTALLABLE_MSBUILD_VERSION), "The test aims to install 2 different versions of the Scanner MSBuild, but they are not.");
    MSBuildScannerInstallation.install(jenkins, OLDEST_INSTALLABLE_MSBUILD_VERSION, false);
    MSBuildScannerInstallation.install(jenkins, LATEST_INSTALLABLE_MSBUILD_VERSION, true);
    jenkinsOrch.configureSonarInstallation(ORCHESTRATOR);

    String jobName = "csharp-core";
    String projectKey = "csharp-core";
    assertThat(getProject(projectKey)).isNull();
    jenkinsOrch
      .newFreestyleJobWithScannerForMsBuild(jobName, null, consoleNetCoreFolder, projectKey, "CSharp NetCore", "1.0", LATEST_INSTALLABLE_MSBUILD_VERSION, "NetCoreConsoleApp.sln", true)
      .executeJob(jobName);

    waitForComputationOnSQServer();
    assertThat(getProject(projectKey)).isNotNull();
    assertSonarUrlOnJob(jobName, projectKey);
  }

  @Test
  @WithPlugins({"maven-plugin"})
  public void scan_project_using_maven_plugin() {
    jenkinsOrch.configureSonarInstallation(ORCHESTRATOR)
      .configureMaven(ORCHESTRATOR);

    String jobName = "abacus-maven";
    assertThat(getProject(MVN_PROJECT_KEY)).isNull();

    jenkinsOrch
      .newMavenJobConfig(jobName, new File("projects", "abacus"))
      .activateSonarPostBuildMaven()
      .save();

    jenkinsOrch.executeJob(jobName);
    waitForComputationOnSQServer();
    assertThat(getProject(MVN_PROJECT_KEY)).isNotNull();
    assertSonarUrlOnJob(jobName, MVN_PROJECT_KEY);
  }

  @Test
  @WithPlugins({"maven-plugin"})
  public void scan_project_using_maven_plugin_passed_quality_gate() {
    jenkinsOrch.configureSonarInstallation(ORCHESTRATOR)
      .configureMaven(ORCHESTRATOR);

    String jobName = "abacus-freestyle";
    assertThat(getProject(MVN_PROJECT_KEY)).isNull();

    jenkinsOrch
      .newFreestyleJobConfig(jobName, new File("projects", "abacus"))
      .addMavenBuildStep("clean package")
      .activateSonarPostBuildMaven()
      .save();

    jenkinsOrch.executeJob(jobName);
    waitForComputationOnSQServer();
    assertThat(getProject(MVN_PROJECT_KEY)).isNotNull();
    assertSonarUrlOnJob(jobName, MVN_PROJECT_KEY);
    jenkinsOrch.assertPassedQGOnProjectPage(jobName);
  }

  @Test
  @WithPlugins({"maven-plugin"})
  public void scan_project_using_maven_plugin_with_variable_injection() throws FailedExecutionException {
    jenkinsOrch.configureSonarInstallation(ORCHESTRATOR)
      .configureMaven(ORCHESTRATOR);

    String jobName = "abacus-freestyle-vars";
    assertThat(getProject(MVN_PROJECT_KEY)).isNull();

    jenkinsOrch.enableInjectionVars(true)
      .newFreestyleJobConfig(jobName, new File("projects", "abacus"))
      .configureSonarBuildWrapper()
      .addMavenBuildStep("clean package")
      .addSonarMavenBuildStep(ORCHESTRATOR)
      .save();

    jenkinsOrch.executeJob(jobName);
    waitForComputationOnSQServer();
    assertThat(getProject(MVN_PROJECT_KEY)).isNotNull();
    assertSonarUrlOnJob(jobName, MVN_PROJECT_KEY);
    jenkinsOrch.assertPassedQGOnProjectPage(jobName);
  }

  @Test
  @WithPlugins("workflow-aggregator")
  public void env_wrapper_missing_sq_env_vars_should_be_nonexistent() throws JenkinsUtils.FailedExecutionException {
    String logs = runAndGetLogs("no-withSonarQubeEnv", DUMP_ENV_VARS_PIPELINE_CMD);
    try {
      verifySonarqubeEnvVarsExist(logs);
    } catch (AssertionError e) {
      return;
    }
    fail("SonarQube env variables should not exist without withSonarQubeEnv wrapper");
  }

  @Test
  @WithPlugins("workflow-aggregator")
  public void env_wrapper_without_params_should_inject_sq_vars() {
    jenkinsOrch.configureSonarInstallation(ORCHESTRATOR);

    String script = "withSonarQubeEnv { " + DUMP_ENV_VARS_PIPELINE_CMD + " }";
    runAndVerifySonarqubeEnvVarsExist("withSonarQubeEnv-parameterless", script);
  }

  @Test
  @WithPlugins("workflow-aggregator")
  public void env_wrapper_with_specific_sq_should_inject_sq_vars() {
    jenkinsOrch.configureSonarInstallation(ORCHESTRATOR);

    String script = "withSonarQubeEnv('" + DEFAULT_SONARQUBE_INSTALLATION + "') { " + DUMP_ENV_VARS_PIPELINE_CMD + " }";
    runAndVerifySonarqubeEnvVarsExist("withSonarQubeEnv-SonarQube", script);
  }

  @Test(expected = JenkinsUtils.FailedExecutionException.class)
  @WithPlugins("workflow-aggregator")
  public void env_wrapper_with_nonexistent_sq_should_fail() {
    jenkinsOrch.configureSonarInstallation(ORCHESTRATOR);

    String script = "withSonarQubeEnv('nonexistent') { " + DUMP_ENV_VARS_PIPELINE_CMD + " }";
    runAndVerifySonarqubeEnvVarsExist("withSonarQubeEnv-nonexistent", script);
  }

  @Test
  @WithPlugins("workflow-aggregator")
  public void quality_gate_pipeline_result_passed() {
    SonarScannerInstallation.install(jenkins, SONARQUBE_SCANNER_VERSION);
    jenkinsOrch.configureSonarInstallation(ORCHESTRATOR);

    String script = SonarQubeScriptBuilder.newScript()
      .sonarQubeScannerVersion(SONARQUBE_SCANNER_VERSION)
      .build();

    createPipelineJobFromScript("js-pipeline", script);
    Build buildResult = jenkinsOrch.executeJob("js-pipeline");
    assertThat(buildResult.isSuccess()).isTrue();
  }

  @Test
  @WithPlugins("workflow-aggregator")
  public void quality_gate_pipeline_result_failed() {
    SonarScannerInstallation.install(jenkins, SONARQUBE_SCANNER_VERSION);
    jenkinsOrch.configureSonarInstallation(ORCHESTRATOR);

    String previousDefault = getDefaultQualityGateName();
    Qualitygates.CreateResponse simple = wsClient.qualitygates().create(new org.sonarqube.ws.client.qualitygates.CreateRequest().setName("AlwaysFail"));
    setDefaultQualityGate(simple.getName());
    wsClient.qualitygates().createCondition(new CreateConditionRequest().setGateId(simple.getId()).setMetric("lines").setOp("GT").setError("0"));

    try {
      String script = SonarQubeScriptBuilder.newScript()
        .sonarQubeScannerVersion(SONARQUBE_SCANNER_VERSION)
        .build();
      createPipelineJobFromScript("js-pipeline-ko", script);
      Build buildResult = jenkinsOrch.executeJobQuietly("js-pipeline-ko");

      assertThat(buildResult.isSuccess()).isFalse();

    } finally {
      setDefaultQualityGate(previousDefault);
      wsClient.wsConnector().call(
        new PostRequest("api/qualitygates/destroy").setParam("name", simple.getName())
      );
    }
  }

  private void runAndVerifySonarqubeEnvVarsExist(String jobName, String script) {
    String logs = runAndGetLogs(jobName, script);
    verifySonarqubeEnvVarsExist(logs);
  }

  private void verifySonarqubeEnvVarsExist(String logs) {
    assertThat(logs).contains("SONAR_AUTH_TOKEN=");
    assertThat(logs).contains("SONAR_CONFIG_NAME=" + DEFAULT_SONARQUBE_INSTALLATION);
    assertThat(logs).contains("SONAR_HOST_URL=" + ORCHESTRATOR.getServer().getUrl());
    assertThat(logs).contains("SONAR_MAVEN_GOAL=sonar:sonar");
    assertThat(logs).contains("SONARQUBE_SCANNER_PARAMS={ \"sonar.host.url\" : \"" + StringEscapeUtils.escapeJson(ORCHESTRATOR.getServer().getUrl()) + "");
  }
}
