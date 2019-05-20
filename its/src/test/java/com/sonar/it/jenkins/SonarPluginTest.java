/*
 * Jenkins :: Integration Tests
 * Copyright (C) 2013-2019 SonarSource SA
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

import com.google.common.net.UrlEscapers;
import com.sonar.it.jenkins.JenkinsUtils.FailedExecutionException;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SynchronousAnalyzer;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.MavenLocation;
import java.io.File;
import java.nio.file.Paths;
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
import org.sonar.wsclient.qualitygate.NewCondition;
import org.sonar.wsclient.qualitygate.QualityGate;
import org.sonar.wsclient.qualitygate.QualityGateClient;
import org.sonar.wsclient.services.PropertyDeleteQuery;
import org.sonar.wsclient.services.PropertyUpdateQuery;
import org.sonarqube.ws.Components.Component;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.components.ShowRequest;
import org.sonarqube.ws.client.webhooks.CreateRequest;
import org.sonarqube.ws.client.webhooks.DeleteRequest;
import org.sonarqube.ws.client.webhooks.ListRequest;

import static com.sonar.it.jenkins.JenkinsUtils.DEFAULT_SONARQUBE_INSTALLATION;
import static java.util.Objects.requireNonNull;
import static java.util.regex.Matcher.quoteReplacement;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

@WithPlugins({"sonar", "filesystem_scm", "plain-credentials"})
public class SonarPluginTest extends AbstractJUnitTest {

  private static final String DUMP_ENV_VARS_PIPELINE_CMD = SystemUtils.IS_OS_WINDOWS ? "bat 'set'" : "sh 'env | sort'";
  private static final String GLOBAL_WEBHOOK_PROPERTY = "sonar.webhooks.global";
  private static long DEFAULT_QUALITY_GATE;

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .setSonarVersion(requireNonNull(System.getProperty("sonar.runtimeVersion"), "Please set system property sonar.runtimeVersion"))
    .addPlugin(MavenLocation.of("org.sonarsource.java", "sonar-java-plugin", "LATEST_RELEASE"))
    .addPlugin(MavenLocation.of("org.sonarsource.javascript", "sonar-javascript-plugin", "LATEST_RELEASE"))
    // Needed by Scanner for MSBuild
    .addPlugin(MavenLocation.of("org.sonarsource.dotnet", "sonar-csharp-plugin", "LATEST_RELEASE"))
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
    DEFAULT_QUALITY_GATE = qgClient().list().defaultGate().id();
    // Set up webhook
    wsClient = WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
      .url(ORCHESTRATOR.getServer().getUrl())
      .credentials(Server.ADMIN_LOGIN, Server.ADMIN_PASSWORD)
      .build());
  }

  @Before
  public void setUp() throws Exception {
    ORCHESTRATOR.resetData();
    qgClient().setDefault(DEFAULT_QUALITY_GATE);
    jenkinsOrch = new JenkinsUtils(jenkins, driver);
    jenkinsOrch.configureDefaultQG(ORCHESTRATOR);
    jenkins.open();
    enableWebhook();
  }

  @After
  public void cleanup() {
    disableGlobalWebhooks();
  }

  @Test
  public void testFreestyleJobWithSonarQubeScanner_use_sq_scanner_3_3() {
    SonarScannerInstallation.install(jenkins, "3.3.0.1492");
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
    MSBuildScannerInstallation.install(jenkins, "3.0.0.629", false);
    MSBuildScannerInstallation.install(jenkins, "4.1.0.1148", true);
    jenkinsOrch.configureSonarInstallation(ORCHESTRATOR)
      .configureMSBuild(ORCHESTRATOR);

    String jobName = "csharp";
    String projectKey = "csharp";
    assertThat(getProject(projectKey)).isNull();
    jenkinsOrch
      .newFreestyleJobWithScannerForMsBuild(jobName, null, consoleApp1Folder, projectKey, "CSharp", "1.0", "3.0.0.629", "ConsoleApplication1.sln", false)
      .executeJob(jobName);

    waitForComputationOnSQServer();
    assertThat(getProject(projectKey)).isNotNull();
    assertSonarUrlOnJob(jobName, projectKey);
  }

  @Test
  public void testFreestyleJobWithScannerForMsBuild_NetCore() {
    MSBuildScannerInstallation.install(jenkins, "3.0.0.629", false);
    MSBuildScannerInstallation.install(jenkins, "4.1.0.1148", true);
    jenkinsOrch.configureSonarInstallation(ORCHESTRATOR);

    String jobName = "csharp-core";
    String projectKey = "csharp-core";
    assertThat(getProject(projectKey)).isNull();
    jenkinsOrch
      .newFreestyleJobWithScannerForMsBuild(jobName, null, consoleNetCoreFolder, projectKey, "CSharp NetCore", "1.0", "4.1.0.1148", "NetCoreConsoleApp.sln", true)
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
    MSBuildScannerInstallation.install(jenkins, "3.0.0.629", false);
    jenkinsOrch.configureSonarInstallation(ORCHESTRATOR)
      .configureMSBuild(ORCHESTRATOR);

    String jobName = "msbuild-sq-runner-3_0";
    String projectKey = "msbuild-sq-runner-3_0";
    assertThat(getProject(projectKey)).isNull();
    Build result = jenkinsOrch
      .newFreestyleJobWithScannerForMsBuild(jobName, null, jsFolder, projectKey, "JS with space", "1.0", "3.0.0.629", null, false)
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
    MSBuildScannerInstallation.install(jenkins, "3.0.0.629", false);
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
    String projectKey = "org.codehaus.sonar-plugins:sonar-abacus-plugin";
    assertThat(getProject(projectKey)).isNull();
    jenkinsOrch
      .newMavenJobWithSonar(jobName, new File("projects", "abacus"), null)
      .executeJob(jobName);
    waitForComputationOnSQServer();
    assertThat(getProject(projectKey)).isNotNull();
    assertSonarUrlOnJob(jobName, projectKey);
  }

  @Test
  @WithPlugins({"maven-plugin"})
  public void testVariableInjection() throws JenkinsUtils.FailedExecutionException {
    jenkinsOrch.configureSonarInstallation(ORCHESTRATOR)
      .configureMaven(ORCHESTRATOR);

    String jobName = "abacus-freestyle-vars";
    String projectKey = "org.codehaus.sonar-plugins:sonar-abacus-plugin";
    assertThat(getProject(projectKey)).isNull();

    jenkinsOrch.enableInjectionVars(true)
      .newFreestyleJobWithMaven(jobName, new File("projects", "abacus"), null, ORCHESTRATOR)
      .executeJob(jobName);
    waitForComputationOnSQServer();
    assertThat(getProject(projectKey)).isNotNull();
    assertSonarUrlOnJob(jobName, projectKey);
    jenkinsOrch.assertQGOnProjectPage(jobName);
  }

  @Test
  @WithPlugins({"maven-plugin"})
  public void testFreestyleJobWithSonarMaven() {
    jenkinsOrch.configureSonarInstallation(ORCHESTRATOR)
      .configureMaven(ORCHESTRATOR);

    String jobName = "abacus-freestyle";
    String projectKey = "org.codehaus.sonar-plugins:sonar-abacus-plugin";
    assertThat(getProject(projectKey)).isNull();
    jenkinsOrch
      .newFreestyleJobWithSonar(jobName, new File("projects", "abacus"), null)
      .executeJob(jobName);
    waitForComputationOnSQServer();
    assertThat(getProject(projectKey)).isNotNull();
    assertSonarUrlOnJob(jobName, projectKey);
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
    MSBuildScannerInstallation.install(jenkins, "3.0.0.629", false);
    jenkinsOrch.configureSonarInstallation(ORCHESTRATOR);

    String script = "withSonarQubeEnv('" + DEFAULT_SONARQUBE_INSTALLATION + "') {\n"
      + "  bat 'xcopy " + Paths.get("projects/csharp").toAbsolutePath().toString().replaceAll("\\\\", quoteReplacement("\\\\")) + " . /s /e /y'\n"
      + "  def sqScannerMsBuildHome = tool 'Scanner for MSBuild 3.0.0.629'\n"
      + "  bat \"${sqScannerMsBuildHome}\\\\MSBuild.SonarQube.Runner.exe begin /k:csharp /n:CSharp /v:1.0\"\n"
      + "  bat '\\\"%MSBUILD_PATH%\\\" /t:Rebuild'\n"
      + "  bat \"${sqScannerMsBuildHome}\\\\MSBuild.SonarQube.Runner.exe end\"\n"
      + "}";
    assertThat(runAndGetLogs("csharp-pipeline", script)).contains("ANALYSIS SUCCESSFUL, you can browse");
  }

  @Test
  @WithPlugins("workflow-aggregator")
  public void qualitygate_pipeline_ok() {
    SonarScannerInstallation.install(jenkins, "3.3.0.1492");
    jenkinsOrch.configureSonarInstallation(ORCHESTRATOR);

    StringBuilder script = new StringBuilder();
    script.append("withSonarQubeEnv('" + DEFAULT_SONARQUBE_INSTALLATION + "') {\n");
    if (SystemUtils.IS_OS_WINDOWS) {
      script.append("  bat 'xcopy " + Paths.get("projects/js").toAbsolutePath().toString().replaceAll("\\\\", quoteReplacement("\\\\")) + " . /s /e /y'\n");
    } else {
      script.append("  sh 'cp -rf " + Paths.get("projects/js").toAbsolutePath().toString() + "/. .'\n");
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
    SonarScannerInstallation.install(jenkins, "3.3.0.1492");
    jenkinsOrch.configureSonarInstallation(ORCHESTRATOR);

    Long previousDefault = qgClient().list().defaultGate().id();
    QualityGate simple = qgClient().create("AlwaysFail");
    qgClient().setDefault(simple.id());
    qgClient().createCondition(NewCondition.create(simple.id()).metricKey("lines").operator("GT").errorThreshold("0"));

    try {
      StringBuilder script = new StringBuilder();
      script.append("withSonarQubeEnv('" + DEFAULT_SONARQUBE_INSTALLATION + "') {\n");
      if (SystemUtils.IS_OS_WINDOWS) {
        script.append("  bat 'xcopy " + Paths.get("projects/js").toAbsolutePath().toString().replaceAll("\\\\", quoteReplacement("\\\\")) + " . /s /e /y'\n");
      } else {
        script.append("  sh 'cp -rf " + Paths.get("projects/js").toAbsolutePath().toString() + "/. .'\n");
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
      qgClient().setDefault(previousDefault);
      qgClient().destroy(simple.id());
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

  private static QualityGateClient qgClient() {
    return ORCHESTRATOR.getServer().adminWsClient().qualityGateClient();
  }

  private void assertSonarUrlOnJob(String jobName, String projectKey) {
    if (ORCHESTRATOR.getServer().version().isGreaterThan(6, 7)) {
      assertThat(jenkinsOrch.getSonarUrlOnJob(jobName)).isEqualTo(ORCHESTRATOR.getServer().getUrl() + "/dashboard?id=" + UrlEscapers.urlFormParameterEscaper().escape(projectKey));
    } else {
      assertThat(jenkinsOrch.getSonarUrlOnJob(jobName)).isEqualTo(ORCHESTRATOR.getServer().getUrl() + "/dashboard/index/" + UrlEscapers.urlPathSegmentEscaper().escape(projectKey));
    }
  }

  private static void waitForComputationOnSQServer() {
    new SynchronousAnalyzer(ORCHESTRATOR.getServer()).waitForDone();
  }

  private void enableWebhook() {
    String url = StringUtils.removeEnd(jenkins.getCurrentUrl(), "/") + "/sonarqube-webhook/";
    if (ORCHESTRATOR.getServer().version().isGreaterThanOrEquals(7, 1)) {
      // SONAR-9058
      wsClient.webhooks().create(new CreateRequest()
        .setName("Jenkins")
        .setUrl(url));
    } else {
      setProperty(GLOBAL_WEBHOOK_PROPERTY + ".1.name", "Jenkins");
      setProperty(GLOBAL_WEBHOOK_PROPERTY + ".1.url", url);
      setProperty(GLOBAL_WEBHOOK_PROPERTY, "1");
    }
  }

  private static void setProperty(String key, String value) {
    ORCHESTRATOR.getServer().getAdminWsClient().update(new PropertyUpdateQuery(key, value));
  }

  private static void disableGlobalWebhooks() {
    if (ORCHESTRATOR.getServer().version().isGreaterThanOrEquals(7, 1)) {
      // SONAR-9058
      wsClient.webhooks().list(new ListRequest()).getWebhooksList().forEach(p -> wsClient.webhooks().delete(new DeleteRequest().setWebhook(p.getKey())));
    } else {
      ORCHESTRATOR.getServer().getAdminWsClient().delete(new PropertyDeleteQuery(GLOBAL_WEBHOOK_PROPERTY));
    }
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

}
