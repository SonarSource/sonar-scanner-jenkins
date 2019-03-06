/*
 * SonarQube Scanner for Jenkins
 * Copyright (C) 2007-2019 SonarSource SA
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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import hudson.plugins.sonar.SonarGlobalConfiguration;
import hudson.plugins.sonar.SonarInstallation;
import hudson.plugins.sonar.utils.SonarUtils;
import org.apache.commons.lang.SystemUtils;
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
  public void waitForQualityGate_TwoAnalysis() {
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        handler.status1 = "PENDING";
        handler.status2 = "PENDING";
        QueueTaskFuture<WorkflowRun> pipeline = submitPipeline(true, true);
        WorkflowRun b = pipeline.waitForStart();

        submitWebHook(FAKE_TASK_ID_1, "SUCCESS", "OK", b);
        submitWebHook(FAKE_TASK_ID_2, "SUCCESS", "OK", b);
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

        submitWebHook(FAKE_TASK_ID_1, "FAILED", null, b);
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
        submitWebHook(FAKE_TASK_ID_1, "SUCCESS", "OK", b);
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

  private void submitWebHook(String taskId, String endTaskStatus, String qgStatus, WorkflowRun b) throws InterruptedException, IOException, SAXException {
    waitForStepToWait(b);

    story.j.postJSON("sonarqube-webhook/", "{\n" +
      "\"taskId\":\"" + taskId + "\",\n" +
      "\"status\":\"" + endTaskStatus + "\",\n" +
      "\"qualityGate\":{\"status\":\"" + qgStatus + "\"}\n" +
      "}");
  }

  private void waitForStepToWait(WorkflowRun b) throws InterruptedException {
    // Wait for the step to register to the webhook listener
    while (SonarQubeWebHook.get().listeners.isEmpty() && b.isBuilding()) {
      Thread.sleep(500);
    }
  }

  private QueueTaskFuture<WorkflowRun> submitPipeline(boolean specifyServer, boolean twoProjects) throws IOException, InterruptedException, ExecutionException {
    SonarQubeWebHook.get().listeners.clear();
    String serverUrl = "http://localhost:" + port + "/sonarqube";
    story.j.jenkins.getDescriptorByType(SonarGlobalConfiguration.class)
      .setInstallations(
        new SonarInstallation(SONAR_INSTALLATION_NAME, serverUrl, null, null, null, null, null, null));
    WorkflowJob p = story.j.jenkins.createProject(WorkflowJob.class, JOB_NAME);
    String reportTaskContent1 = "dashboardUrl=" + serverUrl + "/dashboard\\n"
      + "ceTaskId=" + FAKE_TASK_ID_1 + "\\nserverUrl=" + serverUrl + "\\nprojectKey=foo";
    String reportTaskContent2 = "dashboardUrl=" + serverUrl + "/dashboard\\n"
      + "ceTaskId=" + FAKE_TASK_ID_2 + "\\nserverUrl=" + serverUrl + "\\nprojectKey=foo";
    p.setDefinition(new CpsFlowDefinition(
      script(reportTaskContent1, reportTaskContent2, specifyServer, twoProjects),
      true));
    return p.scheduleBuild2(0);
  }

  private String script(String reportTaskContent1, String reportTaskContent2, boolean specifyServer, boolean twoProjects) {
    if (this.declarative) {
      StringBuilder pipeline = new StringBuilder();
      pipeline.append("pipeline {\n");
      pipeline.append("  agent none\n");
      pipeline.append("  stages {\n");
      declarativePipelineOneProject(1, reportTaskContent1, pipeline);
      if (twoProjects) {
        declarativePipelineOneProject(2, reportTaskContent2, pipeline);
      }
      pipeline.append("  }\n");
      pipeline.append("}");
      return pipeline.toString();
    } else {
      StringBuilder pipeline = new StringBuilder();
      scriptedPipelineOneProject(1, reportTaskContent1, specifyServer, pipeline);
      if (twoProjects) {
        scriptedPipelineOneProject(2, reportTaskContent2, specifyServer, pipeline);
      }
      return pipeline.toString();
    }
  }

  private void declarativePipelineOneProject(int id, String reportTaskContent, StringBuilder pipeline) {
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
    pipeline.append("        waitForQualityGate abortPipeline: true\n");
    pipeline.append("      }\n");
    pipeline.append("    }\n");
  }

  private void scriptedPipelineOneProject(int id, String reportTaskContent, boolean specifyServer, StringBuilder pipeline) {
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
    pipeline.append("def qg" + id + " = waitForQualityGate();\n");
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
