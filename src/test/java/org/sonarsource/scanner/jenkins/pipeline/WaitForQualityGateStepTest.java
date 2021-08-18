/*
 * SonarQube Scanner for Jenkins
 * Copyright (C) 2007-2021 SonarSource SA
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
package org.sonarsource.scanner.jenkins.pipeline;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import hudson.plugins.sonar.SonarGlobalConfiguration;
import hudson.plugins.sonar.SonarInstallation;
import hudson.plugins.sonar.utils.SonarUtils;
import hudson.util.Secret;
import javax.annotation.Nullable;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang.SystemUtils;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.model.Statement;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.RestartableJenkinsRule;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class WaitForQualityGateStepTest {

  @Parameters(name = "declarative: {0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
      {true}, {false}
    });
  }

  private boolean declarative;

  public WaitForQualityGateStepTest(boolean declarative) {
    this.declarative = declarative;
  }

  private static final String JOB_NAME = "p";
  private static final String FAKE_TASK_ID_1 = "fakeTaskId1";
  private static final String FAKE_ANALYSIS_ID_1 = "123456";
  private static final String FAKE_TASK_ID_2 = "fakeTaskId2";
  private static final String FAKE_ANALYSIS_ID_2 = "7891011";
  private static final String WEBHOOK_SECRET = "secret";
  private static final String WEBHOOK_SECRET_ID = "secretId";

  public static final String SONAR_INSTALLATION_NAME = "default";

  @ClassRule
  public static BuildWatcher buildWatcher = new BuildWatcher();
  @Rule
  public RestartableJenkinsRule story = new RestartableJenkinsRule();

  private static int port;

  private static MyHandler handler;

  @BeforeClass
  public static void startFakeSqServer() throws Exception {
    port = NetworkUtils.getNextAvailablePort();
    HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
    handler = new MyHandler();
    server.createContext("/sonarqube", handler);
    server.setExecutor(null);
    server.start();
  }

  @Test
  public void getWebhookSecretId() {
    WaitForQualityGateStep waitForQualityGateStep = new WaitForQualityGateStep(true);
    waitForQualityGateStep.setWebhookSecretId("AV4p2424BJY0hh5C8Sya1ZS234234");
    assertThat(waitForQualityGateStep.getWebhookSecretId()).isEqualTo("AV4p2424BJY0hh5C8Sya1ZS234234");
  }

  @Test
  public void failIfNoTaskIdInContext() {
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        SonarQubeWebHook.get().listeners.clear();
        WorkflowJob p = story.j.jenkins.createProject(WorkflowJob.class, JOB_NAME);
        p.setDefinition(new CpsFlowDefinition("waitForQualityGate()", true));
        WorkflowRun r = story.j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0));
        story.j.assertLogContains("No previous SonarQube analysis found on this pipeline execution.", r);
      }
    });
  }

  @Test
  public void waitForQualityGateOk() {
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        handler.status1 = "PENDING";
        QueueTaskFuture<WorkflowRun> pipeline = submitPipeline(true, false);
        WorkflowRun b = pipeline.waitForStart();

        submitWebHook("another task", "FAILURE", "KO", b);
        submitWebHook(FAKE_TASK_ID_1, "SUCCESS", "OK", b);
        story.j.assertBuildStatusSuccess(pipeline);
      }
    });
  }

  @Test
  public void waitForQualityGateOk_webhook_received_before_wait_started() {
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        handler.status1 = "PENDING";
        QueueTaskFuture<WorkflowRun> pipeline = submitPipeline(true, false);

        submitWebHook("another task", "FAILURE", "KO");
        submitWebHook(FAKE_TASK_ID_1, "SUCCESS", "OK");

        pipeline.waitForStart();
        story.j.assertBuildStatusSuccess(pipeline);
      }
    });
  }

  @Test
  public void waitForQualityGateKo() {
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        handler.status1 = "PENDING";
        QueueTaskFuture<WorkflowRun> pipeline = submitPipeline(false, false);
        WorkflowRun b = pipeline.waitForStart();

        submitWebHook(FAKE_TASK_ID_1, "SUCCESS", "KO", b);
        story.j.assertBuildStatus(Result.FAILURE, pipeline);
      }
    });
  }

  @Test
  public void waitForQualityGate_succeeds_when_no_webhook_secret_id_is_set() {
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        handler.status1 = "PENDING";
        QueueTaskFuture<WorkflowRun> pipeline = submitPipeline(true, false, null);
        WorkflowRun b = pipeline.waitForStart();

        submitWebHook("another task", "FAILURE", "KO", b, WEBHOOK_SECRET);
        submitWebHook(FAKE_TASK_ID_1, "SUCCESS", "OK", b, WEBHOOK_SECRET);
        story.j.assertBuildStatusSuccess(pipeline);
      }
    });
  }

  @Test
  public void waitForQualityGate_succeeds_when_empty_webhook_secret_id_is_set() {
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        handler.status1 = "PENDING";
        QueueTaskFuture<WorkflowRun> pipeline = submitPipeline(true, false, "");
        WorkflowRun b = pipeline.waitForStart();

        submitWebHook("another task", "FAILURE", "KO", b, WEBHOOK_SECRET);
        submitWebHook(FAKE_TASK_ID_1, "SUCCESS", "OK", b, WEBHOOK_SECRET);
        story.j.assertBuildStatusSuccess(pipeline);
      }
    });
  }

  @Test
  public void waitForQualityGate_fails_when_webhook_secret_id_is_set_but_the_value_is_not_present() {
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        handler.status1 = "PENDING";
        QueueTaskFuture<WorkflowRun> pipeline = submitPipeline(false, false, WEBHOOK_SECRET_ID);
        WorkflowRun b = pipeline.waitForStart();

        submitWebHook(FAKE_TASK_ID_1, "SUCCESS", "KO", b, WEBHOOK_SECRET);
        story.j.assertBuildStatus(Result.FAILURE, pipeline);
        story.j.assertLogContains("A webhook secret id was configured, but the corresponding credential could not be found", b);
      }
    });
  }

  @Test
  public void waitForQualityGate_succeeds_when_correct_webhook_secret_is_set() {
    addWebhookSecretToCredentials(WEBHOOK_SECRET);
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        handler.status1 = "PENDING";
        QueueTaskFuture<WorkflowRun> pipeline = submitPipeline(true, false, WEBHOOK_SECRET_ID);
        WorkflowRun b = pipeline.waitForStart();

        submitWebHook("another task", "FAILURE", "KO", b, WEBHOOK_SECRET);
        submitWebHook(FAKE_TASK_ID_1, "SUCCESS", "OK", b, WEBHOOK_SECRET);
        story.j.assertBuildStatusSuccess(pipeline);
        story.j.assertLogContains("The incoming webhook matched the configured webhook secret", b);
      }
    });
  }

  @Test
  public void waitForQualityGate_fails_if_secret_that_is_given_is_different_then_secret_used_to_encode_webhook() {
    addWebhookSecretToCredentials("other secret value");
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        handler.status1 = "PENDING";
        QueueTaskFuture<WorkflowRun> pipeline = submitPipeline(true, false, WEBHOOK_SECRET_ID);
        WorkflowRun b = pipeline.waitForStart();

        submitWebHook("another task", "FAILURE", "KO", b, WEBHOOK_SECRET);
        submitWebHook(FAKE_TASK_ID_1, "SUCCESS", "OK", b, WEBHOOK_SECRET);
        story.j.assertBuildStatus(Result.FAILURE, pipeline);
      }
    });
  }

  @Test
  public void waitForQualityGate_TwoAnalysis() {
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        handler.status1 = "PENDING";
        handler.status2 = "PENDING";
        QueueTaskFuture<WorkflowRun> pipeline = submitPipeline(true, true);
        WorkflowRun b = pipeline.waitForStart();

        submitWebHook(FAKE_TASK_ID_1, "SUCCESS", "OK", b, WEBHOOK_SECRET);
        submitWebHook(FAKE_TASK_ID_2, "SUCCESS", "OK", b, WEBHOOK_SECRET);
        story.j.assertBuildStatusSuccess(pipeline);
      }
    });
  }

  @Test
  public void waitForQualityGateCancelPipeline() {
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        handler.status1 = "PENDING";
        QueueTaskFuture<WorkflowRun> pipeline = submitPipeline(false, false);
        WorkflowRun b = pipeline.waitForStart();
        waitForStepToWait(b);
        b.doStop();
        story.j.assertBuildStatus(Result.ABORTED, pipeline);
        assertThat(SonarQubeWebHook.get().listeners).isEmpty();
      }
    });
  }

  @Test
  public void waitForQualityGateTaskFailure() {
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        handler.status1 = "PENDING";
        QueueTaskFuture<WorkflowRun> pipeline = submitPipeline(false, false);
        WorkflowRun b = pipeline.waitForStart();
        waitForStepToWait(b);

        submitWebHook(FAKE_TASK_ID_1, "FAILED", null, b, WEBHOOK_SECRET);
        WorkflowRun r = story.j.assertBuildStatus(Result.FAILURE, pipeline);
        story.j.assertLogContains("SonarQube analysis '" + FAKE_TASK_ID_1 + "' failed: FAILED", r);
      }
    });
  }

  @Test
  public void finishEarlyOk() {
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        handler.status1 = "SUCCESS";
        handler.analysisId1 = FAKE_ANALYSIS_ID_1;
        handler.qgStatus1 = "OK";
        QueueTaskFuture<WorkflowRun> pipeline = submitPipeline(false, false);
        story.j.assertBuildStatusSuccess(pipeline);
      }
    });
  }

  @Test
  public void finishEarlyKo() {
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        handler.status1 = "SUCCESS";
        handler.analysisId1 = FAKE_ANALYSIS_ID_1;
        handler.qgStatus1 = "KO";
        QueueTaskFuture<WorkflowRun> pipeline = submitPipeline(false, false);
        story.j.assertBuildStatus(Result.FAILURE, pipeline);
      }
    });
  }

  @Test
  public void finishEarlyFail() {
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        handler.status1 = "FAILED";
        QueueTaskFuture<WorkflowRun> pipeline = submitPipeline(false, false);

        WorkflowRun run = story.j.assertBuildStatus(Result.FAILURE, pipeline);
        story.j.assertLogContains("SonarQube task '" + FAKE_TASK_ID_1 + "' status is 'FAILED'", run);
      }
    });
  }

  @Test
  public void waitForQualityGateOk_survive_restart() {
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        handler.status1 = "PENDING";
        QueueTaskFuture<WorkflowRun> pipeline = submitPipeline(false, false);
        WorkflowRun b = pipeline.waitForStart();
        waitForStepToWait(b);
      }
    });
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        WorkflowJob p = story.j.jenkins.getItemByFullName(JOB_NAME, WorkflowJob.class);
        WorkflowRun b = p.getLastBuild();
        submitWebHook(FAKE_TASK_ID_1, "SUCCESS", "OK", b, WEBHOOK_SECRET);
        story.j.assertBuildStatusSuccess(story.j.waitForCompletion(b));
      }
    });
  }

  @Test
  public void waitForQualityGateOk_recheck_ws_on_restart() {
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        handler.status1 = "PENDING";
        QueueTaskFuture<WorkflowRun> pipeline = submitPipeline(false, false);
        WorkflowRun b = pipeline.waitForStart();
        waitForStepToWait(b);

        handler.status1 = "SUCCESS";
        handler.analysisId1 = FAKE_ANALYSIS_ID_1;
        handler.qgStatus1 = "OK";
      }
    });
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        WorkflowJob p = story.j.jenkins.getItemByFullName(JOB_NAME, WorkflowJob.class);
        WorkflowRun b = p.getLastBuild();
        story.j.assertBuildStatusSuccess(story.j.waitForCompletion(b));
      }
    });
  }

  private void addWebhookSecretToCredentials(String secret) {
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        StringCredentialsImpl c = new StringCredentialsImpl(CredentialsScope.GLOBAL, WEBHOOK_SECRET_ID, "sample", Secret.fromString(secret));
        CredentialsProvider.lookupStores(story.j.jenkins).iterator().next().addCredentials(Domain.global(), c);
      }
    });
  }

  private void submitWebHook(String taskId, String endTaskStatus, String qgStatus, WorkflowRun b, String secret) throws InterruptedException, IOException {
    waitForStepToWait(b);

    String payload = "{\n" +
      "\"taskId\":\"" + taskId + "\",\n" +
      "\"status\":\"" + endTaskStatus + "\",\n" +
      "\"qualityGate\":{\"status\":\"" + qgStatus + "\"}\n" +
      "}";
    OkHttpClient client = new OkHttpClient();
    Request req = new Request.Builder()
      .url(story.j.getURL().toExternalForm() + "sonarqube-webhook/")
      .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), payload))
      .addHeader("X-Sonar-Webhook-HMAC-SHA256", new HmacUtils(HmacAlgorithms.HMAC_SHA_256, secret).hmacHex(payload))
      .build();
    client.newCall(req).execute();
  }

  private void submitWebHook(String taskId, String endTaskStatus, String qgStatus, WorkflowRun b) throws InterruptedException, IOException {
    waitForStepToWait(b);

    submitWebHook(taskId, endTaskStatus, qgStatus);
  }

  private void submitWebHook(String taskId, String endTaskStatus, String qgStatus) throws IOException {
    String payload = "{\n" +
      "\"taskId\":\"" + taskId + "\",\n" +
      "\"status\":\"" + endTaskStatus + "\",\n" +
      "\"qualityGate\":{\"status\":\"" + qgStatus + "\"}\n" +
      "}";

    Request req = new Request.Builder()
      .url(story.j.getURL().toExternalForm() + "sonarqube-webhook/")
      .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), payload))
      .build();
    OkHttpClient client = new OkHttpClient();
    client.newCall(req).execute();
  }


  private void waitForStepToWait(WorkflowRun b) throws InterruptedException {
    // Wait for the step to register to the webhook listener
    while (SonarQubeWebHook.get().listeners.isEmpty() && b.isBuilding()) {
      Thread.sleep(500);
    }
  }

  private QueueTaskFuture<WorkflowRun> submitPipeline(boolean specifyServer, boolean twoProjects) throws IOException {
    return submitPipeline(specifyServer, twoProjects, null);
  }

  private QueueTaskFuture<WorkflowRun> submitPipeline(boolean specifyServer, boolean twoProjects, @Nullable String webhookSecretId) throws IOException {
    SonarQubeWebHook.get().listeners.clear();
    story.j.jenkins.getDescriptorByType(SonarGlobalConfiguration.class)
      .setInstallations(
        new SonarInstallation(SONAR_INSTALLATION_NAME, "http://localhost:" + port + "/sonarqube", null, null, null, null, null, null, null));
    WorkflowJob p = story.j.jenkins.createProject(WorkflowJob.class, JOB_NAME);
    // Use a fake serverUrl to validate that the installation url is used by the wait step
    String serverUrl = "http://sonarqube.example.com";
    String reportTaskContent1 = "dashboardUrl=" + serverUrl + "/dashboard\\n"
      + "ceTaskId=" + FAKE_TASK_ID_1 + "\\nserverUrl=" + serverUrl + "\\nprojectKey=foo";
    String reportTaskContent2 = "dashboardUrl=" + serverUrl + "/dashboard\\n"
      + "ceTaskId=" + FAKE_TASK_ID_2 + "\\nserverUrl=" + serverUrl + "\\nprojectKey=foo";
    p.setDefinition(new CpsFlowDefinition(
      script(reportTaskContent1, reportTaskContent2, specifyServer, twoProjects, webhookSecretId),
      true));
    return p.scheduleBuild2(0);
  }

  private String script(String reportTaskContent1, String reportTaskContent2, boolean specifyServer, boolean twoProjects, @Nullable String webhookSecretId) {
    if (this.declarative) {
      StringBuilder pipeline = new StringBuilder();
      pipeline.append("pipeline {\n");
      pipeline.append("  agent none\n");
      pipeline.append("  stages {\n");
      declarativePipelineOneProject(1, reportTaskContent1, pipeline, webhookSecretId);
      if (twoProjects) {
        declarativePipelineOneProject(2, reportTaskContent2, pipeline, webhookSecretId);
      }
      pipeline.append("  }\n");
      pipeline.append("}");
      return pipeline.toString();
    } else {
      StringBuilder pipeline = new StringBuilder();
      scriptedPipelineOneProject(1, reportTaskContent1, specifyServer, pipeline, webhookSecretId);
      if (twoProjects) {
        scriptedPipelineOneProject(2, reportTaskContent2, specifyServer, pipeline, webhookSecretId);
      }
      return pipeline.toString();
    }
  }

  private void declarativePipelineOneProject(int id, String reportTaskContent, StringBuilder pipeline, @Nullable String webhookSecretId) {
    pipeline.append("    stage(\"Scan " + id + "\") {\n");
    pipeline.append("      agent any\n");
    pipeline.append("      steps {\n");
    pipeline.append("        dir(path: 'project" + id + "') {\n");
    pipeline.append("          withSonarQubeEnv('" + SONAR_INSTALLATION_NAME + "') {\n");
    pipeline.append("            writeFile file: 'foo/");
    pipeline.append(SonarUtils.REPORT_TASK_FILE_NAME);
    pipeline.append("', text: '");
    pipeline.append(reportTaskContent);
    pipeline.append("', encoding: 'utf-8'\n");
    pipeline.append("            ");
    pipeline.append((SystemUtils.IS_OS_WINDOWS ? "bat" : "sh"));
    pipeline.append(" 'mvn -version'\n");
    pipeline.append("          }\n");
    pipeline.append("        }\n");
    pipeline.append("      }\n");
    pipeline.append("    }\n");
    pipeline.append("    stage(\"Quality Gate " + id + "\") {\n");
    pipeline.append("      steps {\n");
    if (webhookSecretId == null) {
      pipeline.append("        waitForQualityGate abortPipeline: true \n");
    } else {
      pipeline.append("        waitForQualityGate abortPipeline: true, webhookSecretId: '" + webhookSecretId + "' \n");
    }
    pipeline.append("      }\n");
    pipeline.append("    }\n");
  }

  private void scriptedPipelineOneProject(int id, String reportTaskContent, boolean specifyServer, StringBuilder pipeline, @Nullable String webhookSecretId) {
    pipeline.append("node {\n");
    pipeline.append("  dir(path: 'project" + id + "') {\n");
    if (specifyServer) {
      pipeline.append("    withSonarQubeEnv('" + SONAR_INSTALLATION_NAME + "') {\n");
    } else {
      pipeline.append("    withSonarQubeEnv {\n");
    }
    pipeline.append("      writeFile file: 'foo/");
    pipeline.append(SonarUtils.REPORT_TASK_FILE_NAME);
    pipeline.append("', text: '");
    pipeline.append(reportTaskContent);
    pipeline.append("', encoding: 'utf-8'\n");
    pipeline.append("      ");
    pipeline.append((SystemUtils.IS_OS_WINDOWS ? "bat" : "sh"));
    pipeline.append(" 'mvn -version'\n");
    pipeline.append("    }\n");
    pipeline.append("  }\n");
    pipeline.append("}\n");
    if (webhookSecretId == null) {
      pipeline.append("def qg" + id + " = waitForQualityGate();\n");
    } else {
      pipeline.append("def qg" + id + " = waitForQualityGate(webhookSecretId: '"+ webhookSecretId + "');\n");
    }
    pipeline.append("if (qg" + id + ".status != 'OK') {\n");
    pipeline.append("  error 'QG" + id + " failure'\n");
    pipeline.append("}\n");
  }

  static class MyHandler implements HttpHandler {

    String status1 = "PENDING";
    String analysisId1 = null;
    String qgStatus1 = null;
    String status2 = "PENDING";
    String analysisId2 = null;
    String qgStatus2 = null;

    @Override
    public void handle(HttpExchange t) throws IOException {
      if (t.getRequestURI().getPath().equals("/sonarqube/api/server/version")) {
        response(t, 200, "6.7");
        return;
      }
      if (t.getRequestURI().getPath().equals("/sonarqube/api/ce/task")) {
        if (t.getRequestURI().getQuery().equals("id=" + FAKE_TASK_ID_1)) {
          response(t, 200, "{ task: {\"componentKey\": \"project_1\","
            + "\"componentName\": \"Project One\","
            + (analysisId1 != null ? ("\"analysisId\": \"" + analysisId1 + "\",") : "")
            + "\"status\": \"" + status1 + "\"}}");
          return;
        }
        if (t.getRequestURI().getQuery().equals("id=" + FAKE_TASK_ID_2)) {
          response(t, 200, "{ task: {\"componentKey\": \"project_2\","
            + "\"componentName\": \"Project Two\","
            + (analysisId2 != null ? ("\"analysisId\": \"" + analysisId2 + "\",") : "")
            + "\"status\": \"" + status2 + "\"}}");
          return;
        }
      }
      if (t.getRequestURI().getPath().equals("/sonarqube/api/qualitygates/project_status")) {
        if (t.getRequestURI().getQuery().equals("analysisId=" + FAKE_ANALYSIS_ID_1)) {
          response(t, 200, "{ projectStatus: {\"status\": \"" + qgStatus1 + "\"}}");
          return;
        }
        if (t.getRequestURI().getQuery().equals("analysisId=" + FAKE_ANALYSIS_ID_2)) {
          response(t, 200, "{ projectStatus: {\"status\": \"" + qgStatus2 + "\"}}");
          return;
        }
      }
      response(t, 404, "not found");
    }

    private void response(HttpExchange t, int code, String body) throws IOException {
      t.sendResponseHeaders(code, body.length());
      OutputStream os = t.getResponseBody();
      os.write(body.getBytes());
      os.close();
    }
  }

}
