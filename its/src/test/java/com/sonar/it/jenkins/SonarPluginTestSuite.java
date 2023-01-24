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

import com.google.common.net.UrlEscapers;
import com.sonar.it.jenkins.utility.JenkinsUtils;
import com.sonar.it.jenkins.utility.ScannerSupportedVersionProvider;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SynchronousAnalyzer;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.http.HttpMethod;
import com.sonar.orchestrator.locator.FileLocation;
import java.io.File;
import javax.annotation.CheckForNull;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;
import org.jenkinsci.test.acceptance.junit.WithPlugins;
import org.jenkinsci.test.acceptance.po.WorkflowJob;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.sonarqube.ws.Components;
import org.sonarqube.ws.Qualitygates;
import org.sonarqube.ws.Webhooks;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.components.ShowRequest;
import org.sonarqube.ws.client.webhooks.CreateRequest;
import org.sonarqube.ws.client.webhooks.DeleteRequest;
import org.sonarqube.ws.client.webhooks.ListRequest;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

@WithPlugins({"sonar", "credentials@2.6.1", "filesystem_scm"})
public class SonarPluginTestSuite extends AbstractJUnitTest {

  private static final ScannerSupportedVersionProvider SCANNER_VERSION_PROVIDER = new ScannerSupportedVersionProvider();
  private static final String SECRET = "very_secret_secret";
  private static String DEFAULT_QUALITY_GATE_NAME;
  protected static String EARLIEST_JENKINS_SUPPORTED_MS_BUILD_VERSION;
  protected static final String MS_BUILD_RECENT_VERSION = "4.7.1.2311";
  protected static WsClient wsClient;
  protected JenkinsUtils jenkinsOrch;
  protected final File csharpFolder = new File("projects", "csharp");
  protected final File jsFolder = new File("projects", "js");

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .setSonarVersion(requireNonNull(System.getProperty("sonar.runtimeVersion"), "Please set system property sonar.runtimeVersion"))
    // Disable webhook url validation
    .setServerProperty("sonar.validateWebhooks", Boolean.FALSE.toString())
    .keepBundledPlugins()
    .restoreProfileAtStartup(FileLocation.ofClasspath("/com/sonar/it/jenkins/SonarPluginTest/sonar-way-it-profile_java.xml"))
    .build();

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

  @CheckForNull
  protected static Components.Component getProject(String componentKey) {
    try {
      return wsClient.components().show(new ShowRequest().setComponent(componentKey)).getComponent();
    } catch (HttpException e) {
      if (e.code() == 404) {
        return null;
      }
      throw new IllegalStateException(e);
    }
  }

  protected static void waitForComputationOnSQServer() {
    new SynchronousAnalyzer(ORCHESTRATOR.getServer()).waitForDone();
  }

  protected void assertSonarUrlOnJob(String jobName, String projectKey) {
    assertThat(jenkinsOrch.getSonarUrlOnJob(jobName)).isEqualTo(ORCHESTRATOR.getServer().getUrl() + "/dashboard?id=" + UrlEscapers.urlFormParameterEscaper().escape(projectKey));
  }

  protected String runAndGetLogs(String jobName, String script) {
    createPipelineJobFromScript(jobName, script);
    return jenkinsOrch.executeJob(jobName).getConsole();
  }

  protected void createPipelineJobFromScript(String jobName, String script) {
    WorkflowJob job = jenkins.jobs.create(WorkflowJob.class, jobName);
    job.script.set("node { withEnv(['MY_SONAR_URL=" + ORCHESTRATOR.getServer().getUrl() + "']) {" + script + "}}");
    job.save();
  }

  protected static String getDefaultQualityGateName() {
    Qualitygates.ListWsResponse list = wsClient.qualitygates().list(new org.sonarqube.ws.client.qualitygates.ListRequest());

    return list.getQualitygatesList()
        .stream()
        .filter(Qualitygates.ListWsResponse.QualityGate::getIsDefault)
        .findFirst()
        .orElseGet(() -> list.getQualitygates(0)).getName();
  }

  protected void setDefaultQualityGate(String qualityGateName) {
    wsClient.wsConnector().call(
      new PostRequest("api/qualitygates/set_as_default").setParam("name", qualityGateName)
    );
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

  private void reset() {
    ORCHESTRATOR.getServer()
      .newHttpCall("/api/projects/bulk_delete")
      .setAdminCredentials()
      .setMethod(HttpMethod.POST)
      .setParams("q", "sonar")
      .execute();
  }

  private static void disableGlobalWebhooks() {
    wsClient.webhooks().list(new ListRequest()).getWebhooksList().forEach(p -> wsClient.webhooks().delete(new DeleteRequest().setWebhook(p.getKey())));
  }

}
