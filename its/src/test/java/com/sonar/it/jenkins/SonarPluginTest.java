/*
 * Jenkins :: Integration Tests
 * Copyright (C) 2013-2021 SonarSource SA
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

import static com.sonar.it.jenkins.JenkinsUtils.DEFAULT_SONARQUBE_INSTALLATION;
import static java.util.Objects.requireNonNull;
import static java.util.regex.Matcher.quoteReplacement;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import com.google.common.net.UrlEscapers;
import com.sonar.it.jenkins.JenkinsUtils.FailedExecutionException;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SynchronousAnalyzer;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.http.HttpMethod;
import com.sonar.orchestrator.locator.FileLocation;
import java.io.File;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import javax.annotation.CheckForNull;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;
import org.jenkinsci.test.acceptance.junit.Since;
import org.jenkinsci.test.acceptance.junit.WithOS;
import org.jenkinsci.test.acceptance.junit.WithPlugins;
import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.WorkflowJob;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.Components.Component;
import org.sonarqube.ws.Qualitygates;
import org.sonarqube.ws.Webhooks;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.components.ShowRequest;
import org.sonarqube.ws.client.qualitygates.CreateConditionRequest;
import org.sonarqube.ws.client.webhooks.CreateRequest;
import org.sonarqube.ws.client.webhooks.DeleteRequest;
import org.sonarqube.ws.client.webhooks.ListRequest;

@WithPlugins({"sonar", "filesystem_scm", "plain-credentials"})
public class SonarPluginTest extends AbstractJUnitTest {

  private static final ScannerSupportedVersionProvider SCANNER_VERSION_PROVIDER = new ScannerSupportedVersionProvider();

  private static final String DUMP_ENV_VARS_PIPELINE_CMD = SystemUtils.IS_OS_WINDOWS ? "bat 'set'" : "sh 'env | sort'";
  private static final String SECRET = "very_secret_secret";
  private static final String JENKINS_VERSION = "3.3.0.1492";
  private static final String MS_BUILD_RECENT_VERSION = "4.7.1.2311";
  private static final String MVN_PROJECT_KEY = "org.codehaus.sonar-plugins:sonar-abacus-plugin";
  private static String DEFAULT_QUALITY_GATE_NAME;

  private static String EARLIEST_JENKINS_SUPPORTED_MS_BUILD_VERSION;

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .setSonarVersion(requireNonNull(System.getProperty("sonar.runtimeVersion"), "Please set system property sonar.runtimeVersion"))
    // Disable webhook url validation
    .setServerProperty("sonar.validateWebhooks", Boolean.FALSE.toString())
    .keepBundledPlugins()
    .restoreProfileAtStartup(FileLocation.ofClasspath("/com/sonar/it/jenkins/SonarPluginTest/sonar-way-it-profile_java.xml"))
    .build();

  private static WsClient wsClient;

  private final File csharpFolder = new File("projects", "csharp");
  private final File consoleApp1Folder = new File(csharpFolder, "ConsoleApplication1");
  private final File consoleNetCoreFolder = new File(csharpFolder, "NetCoreConsoleApp");
  private final File jsFolder = new File("projects", "js");

  private JenkinsUtils jenkinsOrch;

  @BeforeClass
  public static void setUpJenkins() {
    wsClient = WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
      .url(ORCHESTRATOR.getServer().getUrl())
      .credentials(Server.ADMIN_LOGIN, Server.ADMIN_PASSWORD)
      .build());
    DEFAULT_QUALITY_GATE_NAME = getDefaultQualityGateName();

    EARLIEST_JENKINS_SUPPORTED_MS_BUILD_VERSION = SCANNER_VERSION_PROVIDER
        .getEarliestSupportedVersion("sonar-scanner-msbuild");
  }

  @Before
  public void setUp() {
    setDefaultQualityGate(DEFAULT_QUALITY_GATE_NAME);
    jenkinsOrch = new JenkinsUtils(jenkins, driver);
    jenkinsOrch.configureDefaultQG(ORCHESTRATOR);
    jenkins.open();
    enableWebhook();
  }

  @After
  public void cleanup() {
    reset();
    disableGlobalWebhooks();
  }

  @Test
  public void testFreestyleJobWithSonarQubeScanner_use_sq_scanner_3_3() {
    SonarScannerInstallation.install(jenkins, JENKINS_VERSION);
    jenkinsOrch.configureSonarInstallation(ORCHESTRATOR);

    String jobName = "js-runner-sq-3.3";
    String projectKey = "js-runner-3.3";
    assertThat(getProject(projectKey)).isNull();
    Build result = jenkinsOrch
      .newFreestyleJobWithSQScanner(jobName, "-Duseless=Y", jsFolder, null,
        "sonar.projectKey", projectKey,
        "sonar.projectVersion", "1.0",
        "sonar.projectName", "Abacus",
        "sonar.sources", "src")
      .executeJob(jobName);

    if (SystemUtils.IS_OS_WINDOWS) {
      assertThat(result.getConsole()).contains("sonar-scanner.bat -Duseless=Y");
    } else {
      assertThat(result.getConsole()).contains("sonar-scanner -Duseless=Y");
    }
    assertThat(result.getConsole()).contains("SonarQube Scanner 3.3.0.1492");

    waitForComputationOnSQServer();
    assertThat(getProject(projectKey)).isNotNull();
    assertSonarUrlOnJob(jobName, projectKey);
    jenkinsOrch.assertQGOnProjectPage(jobName);
  }

  @Test
  @WithOS(os = WithOS.OS.WINDOWS)
  @WithPlugins({"msbuild"})
  public void testFreestyleJobWithScannerForMsBuild() throws FailedExecutionException {
    MSBuildScannerInstallation.install(jenkins, MS_BUILD_RECENT_VERSION, false);
    jenkinsOrch.configureSonarInstallation(ORCHESTRATOR)
      .configureMSBuild(ORCHESTRATOR);

    String jobName = "csharp";
    String projectKey = "csharp";
    assertThat(getProject(projectKey)).isNull();
    jenkinsOrch
      .newFreestyleJobWithScannerForMsBuild(jobName, null, consoleApp1Folder, projectKey, "CSharp", "1.0", MS_BUILD_RECENT_VERSION, "ConsoleApplication1.sln", false)
      .executeJob(jobName);

    waitForComputationOnSQServer();
    assertThat(getProject(projectKey)).isNotNull();
    assertSonarUrlOnJob(jobName, projectKey);
  }

  @Test
  public void testFreestyleJobWithScannerForMsBuild_NetCore() {
    MSBuildScannerInstallation.install(jenkins, EARLIEST_JENKINS_SUPPORTED_MS_BUILD_VERSION, false);
    MSBuildScannerInstallation.install(jenkins, MS_BUILD_RECENT_VERSION, true);
    jenkinsOrch.configureSonarInstallation(ORCHESTRATOR);

    String jobName = "csharp-core";
    String projectKey = "csharp-core";
    assertThat(getProject(projectKey)).isNull();
    jenkinsOrch
      .newFreestyleJobWithScannerForMsBuild(jobName, null, consoleNetCoreFolder, projectKey, "CSharp NetCore", "1.0", MS_BUILD_RECENT_VERSION, "NetCoreConsoleApp.sln", true)
      .executeJob(jobName);

    waitForComputationOnSQServer();
    assertThat(getProject(projectKey)).isNotNull();
    assertSonarUrlOnJob(jobName, projectKey);
  }

  @Test
  @WithOS(os = WithOS.OS.WINDOWS)
  @WithPlugins({"msbuild"})
  public void testFreestyleJobWithScannerForMsBuild_3_0() {
    MSBuildScannerInstallation.install(jenkins, "2.3.2.573", false);
    MSBuildScannerInstallation.install(jenkins, EARLIEST_JENKINS_SUPPORTED_MS_BUILD_VERSION, false);
    jenkinsOrch.configureSonarInstallation(ORCHESTRATOR)
      .configureMSBuild(ORCHESTRATOR);

    String jobName = "msbuild-sq-runner-3_0";
    String projectKey = "msbuild-sq-runner-3_0";
    assertThat(getProject(projectKey)).isNull();
    Build result = jenkinsOrch
        .newFreestyleJobWithScannerForMsBuild(jobName, null, jsFolder, projectKey, "JS with space", "1.0",
            EARLIEST_JENKINS_SUPPORTED_MS_BUILD_VERSION, null, false)
        .executeJobQuietly(jobName);

    assertThat(result.getConsole())
      .contains(
        "tools" + File.separator + "hudson.plugins.sonar.MsBuildSQRunnerInstallation" + File.separator + "Scanner_for_MSBuild_3.0.0.629" + File.separator
          + "MSBuild.SonarQube.Runner.exe begin /k:" + projectKey + " \"/n:JS with space\" /v:1.0 /d:sonar.host.url="
          + ORCHESTRATOR.getServer().getUrl());
  }

  @Test
  @WithOS(os = WithOS.OS.WINDOWS)
  @WithPlugins({"msbuild"})
  public void testFreestyleJobWithScannerForMsBuild_2_3_2() {
    MSBuildScannerInstallation.install(jenkins, "2.3.2.573", false);
    MSBuildScannerInstallation.install(jenkins, EARLIEST_JENKINS_SUPPORTED_MS_BUILD_VERSION, false);
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
  // Maven plugin no more installed by default in version 2
  @Since("2.0")
  public void testNoSonarPublisher() {
    String jobName = "noSonarPublisher";
    jenkinsOrch.assertNoSonarPublisher(jobName, new File("projects", "noPublisher"));
  }

  @Test
  @WithPlugins({"maven-plugin"})
  public void testMavenJob() {
    jenkinsOrch.configureSonarInstallation(ORCHESTRATOR)
      .configureMaven(ORCHESTRATOR);

    String jobName = "abacus-maven";
    assertThat(getProject(MVN_PROJECT_KEY)).isNull();
    jenkinsOrch
      .newMavenJobWithSonar(jobName, new File("projects", "abacus"), null)
      .executeJob(jobName);
    waitForComputationOnSQServer();
    assertThat(getProject(MVN_PROJECT_KEY)).isNotNull();
    assertSonarUrlOnJob(jobName, MVN_PROJECT_KEY);
  }

  @Test
  @WithPlugins({"maven-plugin"})
  public void testVariableInjection() throws JenkinsUtils.FailedExecutionException {
    jenkinsOrch.configureSonarInstallation(ORCHESTRATOR)
      .configureMaven(ORCHESTRATOR);

    String jobName = "abacus-freestyle-vars";
    assertThat(getProject(MVN_PROJECT_KEY)).isNull();

    jenkinsOrch.enableInjectionVars(true)
      .newFreestyleJobWithMaven(jobName, new File("projects", "abacus"), null, ORCHESTRATOR)
      .executeJob(jobName);
    waitForComputationOnSQServer();
    assertThat(getProject(MVN_PROJECT_KEY)).isNotNull();
    assertSonarUrlOnJob(jobName, MVN_PROJECT_KEY);
    jenkinsOrch.assertQGOnProjectPage(jobName);
  }

  @Test
  @WithPlugins({"maven-plugin"})
  public void testFreestyleJobWithSonarMaven() {
    jenkinsOrch.configureSonarInstallation(ORCHESTRATOR)
      .configureMaven(ORCHESTRATOR);

    String jobName = "abacus-freestyle";
    assertThat(getProject(MVN_PROJECT_KEY)).isNull();
    jenkinsOrch
      .newFreestyleJobWithSonar(jobName, new File("projects", "abacus"), null)
      .executeJob(jobName);
    waitForComputationOnSQServer();
    assertThat(getProject(MVN_PROJECT_KEY)).isNotNull();
    assertSonarUrlOnJob(jobName, MVN_PROJECT_KEY);
    jenkinsOrch.assertQGOnProjectPage(jobName);
  }

  @Test
  @WithPlugins("workflow-aggregator")
  public void no_sq_vars_without_env_wrapper() throws JenkinsUtils.FailedExecutionException {
    String logs = runAndGetLogs("no-withSonarQubeEnv", DUMP_ENV_VARS_PIPELINE_CMD);
    try {
      verifyEnvVarsExist(logs);
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
    runAndVerifyEnvVarsExist("withSonarQubeEnv-parameterless", script);
  }

  @Test
  @WithPlugins("workflow-aggregator")
  public void env_wrapper_with_specific_sq_should_inject_sq_vars() {
    jenkinsOrch.configureSonarInstallation(ORCHESTRATOR);

    String script = "withSonarQubeEnv('" + DEFAULT_SONARQUBE_INSTALLATION + "') { " + DUMP_ENV_VARS_PIPELINE_CMD + " }";
    runAndVerifyEnvVarsExist("withSonarQubeEnv-SonarQube", script);
  }

  @Test(expected = JenkinsUtils.FailedExecutionException.class)
  @WithPlugins("workflow-aggregator")
  public void env_wrapper_with_nonexistent_sq_should_fail() {
    jenkinsOrch.configureSonarInstallation(ORCHESTRATOR);

    String script = "withSonarQubeEnv('nonexistent') { " + DUMP_ENV_VARS_PIPELINE_CMD + " }";
    runAndVerifyEnvVarsExist("withSonarQubeEnv-nonexistent", script);
  }

  @Test
  @WithPlugins({"workflow-aggregator", "msbuild"})
  @WithOS(os = WithOS.OS.WINDOWS)
  public void msbuild_pipeline() {
    MSBuildScannerInstallation.install(jenkins, EARLIEST_JENKINS_SUPPORTED_MS_BUILD_VERSION, false);
    jenkinsOrch.configureSonarInstallation(ORCHESTRATOR);

    String script = "withSonarQubeEnv('" + DEFAULT_SONARQUBE_INSTALLATION + "') {\n"
      + "  bat 'xcopy " + Paths.get("projects/csharp").toAbsolutePath().toString().replaceAll("\\\\", quoteReplacement("\\\\")) + " . /s /e /y'\n"
      + "  def sqScannerMsBuildHome = tool 'Scanner for MSBuild " + EARLIEST_JENKINS_SUPPORTED_MS_BUILD_VERSION + "'\n"
      + "  bat \"${sqScannerMsBuildHome}\\\\MSBuild.SonarQube.Runner.exe begin /k:csharp /n:CSharp /v:1.0\"\n"
      + "  bat '\\\"%MSBUILD_PATH%\\\" /t:Rebuild'\n"
      + "  bat \"${sqScannerMsBuildHome}\\\\MSBuild.SonarQube.Runner.exe end\"\n"
      + "}";
    assertThat(runAndGetLogs("csharp-pipeline", script)).contains("ANALYSIS SUCCESSFUL, you can browse");
  }

  @Test
  @WithPlugins("workflow-aggregator")
  public void qualitygate_pipeline_ok() {
    SonarScannerInstallation.install(jenkins, JENKINS_VERSION);
    jenkinsOrch.configureSonarInstallation(ORCHESTRATOR);

    StringBuilder script = new StringBuilder();
    script.append("withSonarQubeEnv('" + DEFAULT_SONARQUBE_INSTALLATION + "') {\n");
    if (SystemUtils.IS_OS_WINDOWS) {
      script.append("  bat 'xcopy " + Paths.get("projects/js").toAbsolutePath().toString().replaceAll("\\\\", quoteReplacement("\\\\")) + " . /s /e /y'\n");
    } else {
      script.append("  sh 'cp -rf " + Paths.get("projects/js").toAbsolutePath() + "/. .'\n");
    }
    script.append("  def scannerHome = tool 'SonarQube Scanner 3.3.0.1492'\n");
    if (SystemUtils.IS_OS_WINDOWS) {
      script.append("  bat \"${scannerHome}\\\\bin\\\\sonar-scanner.bat\"\n");
    } else {
      script.append("  sh \"${scannerHome}/bin/sonar-scanner\"\n");
    }
    script.append("}\n");
    script.append("def qg = waitForQualityGate()\n");
    script.append("if (qg.status != 'OK') { error 'Quality gate failure'}\n");
    createPipelineJobFromScript("js-pipeline", script.toString());
    Build buildResult = jenkinsOrch.executeJob("js-pipeline");
    assertThat(buildResult.isSuccess()).isTrue();
  }

  @Test
  @WithPlugins("workflow-aggregator")
  public void qualitygate_pipeline_ko() {
    SonarScannerInstallation.install(jenkins, JENKINS_VERSION);
    jenkinsOrch.configureSonarInstallation(ORCHESTRATOR);

    String previousDefault = getDefaultQualityGateName();
    Qualitygates.CreateResponse simple = wsClient.qualitygates().create(new org.sonarqube.ws.client.qualitygates.CreateRequest().setName("AlwaysFail"));
    setDefaultQualityGate(simple.getName());
    wsClient.qualitygates().createCondition(new CreateConditionRequest().setGateId(simple.getId()).setMetric("lines").setOp("GT").setError("0"));

    try {
      StringBuilder script = new StringBuilder();
      script.append("withSonarQubeEnv('" + DEFAULT_SONARQUBE_INSTALLATION + "') {\n");
      if (SystemUtils.IS_OS_WINDOWS) {
        script.append("  bat 'xcopy " + Paths.get("projects/js").toAbsolutePath().toString().replaceAll("\\\\", quoteReplacement("\\\\")) + " . /s /e /y'\n");
      } else {
        script.append("  sh 'cp -rf " + Paths.get("projects/js").toAbsolutePath() + "/. .'\n");
      }
      script.append("  def scannerHome = tool 'SonarQube Scanner 3.3.0.1492'\n");
      if (SystemUtils.IS_OS_WINDOWS) {
        script.append("  bat \"${scannerHome}\\\\bin\\\\sonar-scanner.bat\"\n");
      } else {
        script.append("  sh \"${scannerHome}/bin/sonar-scanner\"\n");
      }
      script.append("}\n");
      script.append("def qg = waitForQualityGate()\n");
      script.append("if (qg.status != 'OK') { error 'Quality gate failure'}\n");
      createPipelineJobFromScript("js-pipeline-ko", script.toString());
      Build buildResult = jenkinsOrch.executeJobQuietly("js-pipeline-ko");

      assertThat(buildResult.isSuccess()).isFalse();

    } finally {
      setDefaultQualityGate(previousDefault);
      wsClient.wsConnector().call(
          new PostRequest("api/qualitygates/destroy").setParam("name", simple.getName())
      );
    }
  }

  private void runAndVerifyEnvVarsExist(String jobName, String script) {
    String logs = runAndGetLogs(jobName, script);
    verifyEnvVarsExist(logs);
  }

  private String runAndGetLogs(String jobName, String script) {
    createPipelineJobFromScript(jobName, script);
    return jenkinsOrch.executeJob(jobName).getConsole();
  }

  private void verifyEnvVarsExist(String logs) {
    assertThat(logs).contains("SONAR_AUTH_TOKEN=");
    assertThat(logs).contains("SONAR_CONFIG_NAME=" + DEFAULT_SONARQUBE_INSTALLATION);
    assertThat(logs).contains("SONAR_HOST_URL=" + ORCHESTRATOR.getServer().getUrl());
    assertThat(logs).contains("SONAR_MAVEN_GOAL=sonar:sonar");
    assertThat(logs).contains("SONARQUBE_SCANNER_PARAMS={ \"sonar.host.url\" : \"" + StringEscapeUtils.escapeJson(ORCHESTRATOR.getServer().getUrl()) + "");
  }

  private void createPipelineJobFromScript(String jobName, String script) {
    WorkflowJob job = jenkins.jobs.create(WorkflowJob.class, jobName);
    job.script.set("node { withEnv(['MY_SONAR_URL=" + ORCHESTRATOR.getServer().getUrl() + "']) {" + script + "}}");
    job.save();
  }

  private static String getDefaultQualityGateName() {
    Qualitygates.ListWsResponse list = wsClient.qualitygates().list(new org.sonarqube.ws.client.qualitygates.ListRequest());

    return list.getQualitygatesList()
        .stream()
        .filter(Qualitygates.ListWsResponse.QualityGate::getIsDefault)
        .findFirst()
        .orElseGet(() -> list.getQualitygates(0)).getName();
  }

  private void assertSonarUrlOnJob(String jobName, String projectKey) {
      assertThat(jenkinsOrch.getSonarUrlOnJob(jobName)).isEqualTo(ORCHESTRATOR.getServer().getUrl() + "/dashboard?id=" + UrlEscapers.urlFormParameterEscaper().escape(projectKey));
  }

  private static void waitForComputationOnSQServer() {
    new SynchronousAnalyzer(ORCHESTRATOR.getServer()).waitForDone();
  }

  private String enableWebhook() {
    String url = StringUtils.removeEnd(jenkins.getCurrentUrl(), "/") + "/sonarqube-webhook/";
    Webhooks.CreateWsResponse response = wsClient.webhooks().create(new CreateRequest()
      .setName("Jenkins")
      .setUrl(url)
      .setSecret(SECRET)
    );

    return response.getWebhook().getKey();
  }

  public void reset() {
    // We add one day to ensure that today's entries are deleted.
    Instant instant = Instant.now().plus(1, ChronoUnit.DAYS);

    // The expected format is yyyy-MM-dd.
    String currentDateTime = DateTimeFormatter.ISO_LOCAL_DATE
      .withZone(ZoneId.of("UTC"))
      .format(instant);

    ORCHESTRATOR.getServer()
      .newHttpCall("/api/projects/bulk_delete")
      .setAdminCredentials()
      .setMethod(HttpMethod.POST)
      .setParams("analyzedBefore", currentDateTime)
      .execute();
  }

  private static void disableGlobalWebhooks() {
    wsClient.webhooks().list(new ListRequest()).getWebhooksList().forEach(p -> wsClient.webhooks().delete(new DeleteRequest().setWebhook(p.getKey())));
  }

  @CheckForNull
  static Component getProject(String componentKey) {
    try {
      return wsClient.components().show(new ShowRequest().setComponent(componentKey)).getComponent();
    } catch (HttpException e) {
      if (e.code() == 404) {
        return null;
      }
      throw new IllegalStateException(e);
    }
  }

  private void setDefaultQualityGate(String qualityGateName) {
    wsClient.wsConnector().call(
        new PostRequest("api/qualitygates/set_as_default").setParam("name", qualityGateName)
    );
  }

}
