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
import hudson.cli.CLI;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.util.Arrays;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.SystemUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.qualitygate.NewCondition;
import org.sonar.wsclient.qualitygate.QualityGate;
import org.sonar.wsclient.qualitygate.QualityGateClient;
import org.sonar.wsclient.services.PropertyDeleteQuery;
import org.sonar.wsclient.services.PropertyUpdateQuery;

import static com.sonar.it.jenkins.orchestrator.JenkinsOrchestrator.DEFAULT_SONARQUBE_INSTALLATION;
import static java.util.regex.Matcher.quoteReplacement;
import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

public class JenkinsPipelineTest {
  private static final String DUMP_ENV_VARS_PIPELINE_CMD = JenkinsTestSuite.isWindows() ? "bat 'set'" : "sh 'env | sort'";
  private static final String GLOBAL_WEBHOOK_PROPERTY = "sonar.webhooks.global";
  private static long DEFAULT_QUALITY_GATE;
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
      .installPlugin("msbuild")
      // the pipeline plugin -> downloads ~28 other plugins...
      .installPlugin("workflow-aggregator")
      .installPlugin(sqJenkinsPluginLocation)
      .configureSQScannerInstallation("2.8", 0)
      .configureMsBuildSQScanner_installation("3.0.0.629", 0)
      .configureSonarInstallation(orchestrator);
    if (SystemUtils.IS_OS_WINDOWS) {
      jenkins.configureMSBuildInstallation();
    }
    cli = jenkins.getCli();
    // Set up webhook
    enableWebhook();
    DEFAULT_QUALITY_GATE = qgClient().list().defaultGate().id();
  }

  @AfterClass
  public static void cleanup() {
    disableGlobalWebhooks();
  }

  @After
  public void restoreQualityGate() {
    qgClient().setDefault(DEFAULT_QUALITY_GATE);
  }

  @Test
  public void no_sq_vars_without_env_wrapper() throws JenkinsOrchestrator.FailedExecutionException {
    String logs = runAndGetLogs("no-withSonarQubeEnv", "node {" + DUMP_ENV_VARS_PIPELINE_CMD + "}");
    try {
      verifyEnvVarsExist(logs);
    } catch (AssertionError e) {
      return;
    }
    fail("SonarQube env variables should not exist without withSonarQubeEnv wrapper");
  }

  @Test
  public void env_wrapper_without_params_should_inject_sq_vars() {
    String script = "node { withSonarQubeEnv { " + DUMP_ENV_VARS_PIPELINE_CMD + " }}";
    runAndVerifyEnvVarsExist("withSonarQubeEnv-parameterless", script);
  }

  @Test
  public void env_wrapper_with_specific_sq_should_inject_sq_vars() {
    String script = "node {withSonarQubeEnv('" + DEFAULT_SONARQUBE_INSTALLATION + "') { " + DUMP_ENV_VARS_PIPELINE_CMD + " }}";
    runAndVerifyEnvVarsExist("withSonarQubeEnv-SonarQube", script);
  }

  @Test(expected = JenkinsOrchestrator.FailedExecutionException.class)
  public void env_wrapper_with_nonexistent_sq_should_fail() {
    String script = "node {withSonarQubeEnv('nonexistent') { " + DUMP_ENV_VARS_PIPELINE_CMD + " }}";
    runAndVerifyEnvVarsExist("withSonarQubeEnv-nonexistent", script);
  }

  @Test
  public void msbuild_pipeline() {
    assumeTrue(SystemUtils.IS_OS_WINDOWS);
    String script = "node {\n"
      + "withSonarQubeEnv('" + DEFAULT_SONARQUBE_INSTALLATION + "') {\n"
      + "  bat 'xcopy " + Paths.get("projects/csharp").toAbsolutePath().toString().replaceAll("\\\\", quoteReplacement("\\\\")) + " . /s /e /y'\n"
      + "  def sqScannerMsBuildHome = tool 'Scanner for MSBuild 3.0.0.629'\n"
      + "  bat \"${sqScannerMsBuildHome}\\\\MSBuild.SonarQube.Runner.exe begin /k:csharp /n:CSharp /v:1.0\"\n"
      + "  bat '\\\"%MSBUILD_PATH%\\\" /t:Rebuild'\n"
      + "  bat \"${sqScannerMsBuildHome}\\\\MSBuild.SonarQube.Runner.exe end\"\n"
      + "}\n"
      + "}";
    assertThat(runAndGetLogs("csharp-pipeline", script)).contains("ANALYSIS SUCCESSFUL, you can browse");
  }

  @Test
  public void qualitygate_pipeline_ok() {
    assumeTrue(orchestrator.getServer().version().isGreaterThan("6.2"));
    StringBuilder script = new StringBuilder();
    script.append("node {\n");
    script.append("withSonarQubeEnv('" + DEFAULT_SONARQUBE_INSTALLATION + "') {\n");
    if (SystemUtils.IS_OS_WINDOWS) {
      script.append("  bat 'xcopy " + Paths.get("projects/js").toAbsolutePath().toString().replaceAll("\\\\", quoteReplacement("\\\\")) + " . /s /e /y'\n");
    } else {
      script.append("  sh 'cp -rf " + Paths.get("projects/js").toAbsolutePath().toString() + "/. .'\n");
    }
    script.append("  def scannerHome = tool 'SonarQube Scanner 2.8'\n");
    if (SystemUtils.IS_OS_WINDOWS) {
      script.append("  bat \"${scannerHome}\\\\bin\\\\sonar-scanner.bat\"\n");
    } else {
      script.append("  sh \"${scannerHome}/bin/sonar-scanner\"\n");
    }
    script.append("}\n");
    script.append("def qg = waitForQualityGate()\n");
    script.append("if (qg.status != 'OK') { error 'Quality gate failure'}\n");
    script.append("}");
    createPipelineJobFromScript("js-pipeline", script.toString());
    BuildResult buildResult = jenkins.executeJob("js-pipeline");
    assertThat(buildResult.getLastStatus()).isEqualTo(0);
  }

  @Test
  public void qualitygate_pipeline_ko() {
    assumeTrue(orchestrator.getServer().version().isGreaterThan("6.2"));

    Long previousDefault = qgClient().list().defaultGate().id();
    QualityGate simple = qgClient().create("AlwaysFail");
    qgClient().setDefault(simple.id());
    qgClient().createCondition(NewCondition.create(simple.id()).metricKey("lines").operator("GT").errorThreshold("0"));

    try {
      StringBuilder script = new StringBuilder();
      script.append("node {\n");
      script.append("withSonarQubeEnv('" + DEFAULT_SONARQUBE_INSTALLATION + "') {\n");
      if (SystemUtils.IS_OS_WINDOWS) {
        script.append("  bat 'xcopy " + Paths.get("projects/js").toAbsolutePath().toString().replaceAll("\\\\", quoteReplacement("\\\\")) + " . /s /e /y'\n");
      } else {
        script.append("  sh 'cp -rf " + Paths.get("projects/js").toAbsolutePath().toString() + "/. .'\n");
      }
      script.append("  def scannerHome = tool 'SonarQube Scanner 2.8'\n");
      if (SystemUtils.IS_OS_WINDOWS) {
        script.append("  bat \"${scannerHome}\\\\bin\\\\sonar-scanner.bat\"\n");
      } else {
        script.append("  sh \"${scannerHome}/bin/sonar-scanner\"\n");
      }
      script.append("}\n");
      script.append("def qg = waitForQualityGate()\n");
      script.append("if (qg.status != 'OK') { error 'Quality gate failure'}\n");
      script.append("}");
      createPipelineJobFromScript("js-pipeline-ko", script.toString());
      BuildResult buildResult = jenkins.executeJobQuietly("js-pipeline-ko");

      assertThat(buildResult.getLastStatus()).isNotEqualTo(0);

    } finally {
      qgClient().setDefault(previousDefault);
      qgClient().destroy(simple.id());
    }
  }

  private static void enableWebhook() {
    setProperty(GLOBAL_WEBHOOK_PROPERTY + ".1.name", "Jenkins");
    setProperty(GLOBAL_WEBHOOK_PROPERTY + ".1.url", jenkins.getServer().getUrl() + "/sonarqube-webhook/");
    setProperty(GLOBAL_WEBHOOK_PROPERTY, "1");
  }

  private static QualityGateClient qgClient() {
    return orchestrator.getServer().adminWsClient().qualityGateClient();
  }

  private static void setProperty(String key, String value) {
    orchestrator.getServer().getAdminWsClient().update(new PropertyUpdateQuery(key, value));
  }

  private static void disableGlobalWebhooks() {
    orchestrator.getServer().getAdminWsClient().delete(new PropertyDeleteQuery(GLOBAL_WEBHOOK_PROPERTY));
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
    assertThat(logs).contains("SONAR_CONFIG_NAME=" + DEFAULT_SONARQUBE_INSTALLATION);
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
      + "    <script>" + script + "</script>\n"
      + "    <sandbox>true</sandbox>\n"
      + "  </definition>\n"
      + "  <triggers/>\n"
      + "</flow-definition>\n";

    InputStream stdin = new ByteArrayInputStream(config.getBytes());

    NullOutputStream nullOutputStream = new NullOutputStream();
    cli.execute(Arrays.asList("create-job", jobName), stdin, nullOutputStream, nullOutputStream);
  }

}
