/*
 * Jenkins Plugin for SonarQube, open source software quality management tool.
 * mailto:contact AT sonarsource DOT com
 *
 * Jenkins Plugin for SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Jenkins Plugin for SonarQube is distributed in the hope that it will be useful,
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
import hudson.plugins.sonar.SonarGlobalConfiguration;
import hudson.plugins.sonar.SonarInstallation;
import hudson.plugins.sonar.utils.SonarUtils;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.model.Statement;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.RestartableJenkinsRule;
import org.xml.sax.SAXException;

import static org.assertj.core.api.Assertions.assertThat;

public class WaitForQualityGateStepTest {

  private static final String JOB_NAME = "p";
  private static final String FAKE_TASK_ID = "fakeTaskId";
  private static final String FAKE_ANALYSIS_ID = "123456";

  public static final String SONAR_INSTALLATION_NAME = "default";

  @ClassRule
  public static BuildWatcher buildWatcher = new BuildWatcher();
  @Rule
  public RestartableJenkinsRule story = new RestartableJenkinsRule();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

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
        WorkflowRun r = story.j.waitForCompletion(p.scheduleBuild2(0).waitForStart());
        story.j.assertBuildStatus(Result.FAILURE, r);
        story.j.assertLogContains("Unable to get SonarQube task id and/or server name.", r);
      }
    });
  }

  @Test
  public void waitForQualityGateOk() {
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        handler.status = "PENDING";
        WorkflowRun b = submitPipeline();

        submitWebHook("another task", "FAILURE", "KO", b);
        submitWebHook(FAKE_TASK_ID, "SUCCESS", "OK", b);
        story.j.assertBuildStatusSuccess(story.j.waitForCompletion(b));
      }
    });
  }

  @Test
  public void waitForQualityGateKo() {
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        handler.status = "PENDING";
        WorkflowRun b = submitPipeline();

        submitWebHook(FAKE_TASK_ID, "SUCCESS", "KO", b);
        story.j.assertBuildStatus(Result.FAILURE, story.j.waitForCompletion(b));
      }
    });
  }

  @Test
  public void waitForQualityGateCancelPipeline() {
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        handler.status = "PENDING";
        WorkflowRun b = submitPipeline();
        waitForStepToWait();
        b.doStop();
        story.j.assertBuildStatus(Result.ABORTED, story.j.waitForCompletion(b));
        assertThat(SonarQubeWebHook.get().listeners).isEmpty();
      }
    });
  }

  @Test
  public void waitForQualityGateTaskFailure() {
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        handler.status = "PENDING";
        WorkflowRun b = submitPipeline();
        waitForStepToWait();

        submitWebHook(FAKE_TASK_ID, "FAILED", null, b);
        WorkflowRun r = story.j.waitForCompletion(b);
        story.j.assertBuildStatus(Result.FAILURE, r);
        story.j.assertLogContains("SonarQube analysis '" + FAKE_TASK_ID + "' failed: FAILED", r);
      }
    });
  }

  @Test
  public void finishEarlyOk() {
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        handler.status = "SUCCESS";
        handler.analysisId = FAKE_ANALYSIS_ID;
        handler.qgStatus = "OK";
        WorkflowRun b = submitPipeline();

        story.j.assertBuildStatusSuccess(story.j.waitForCompletion(b));
      }
    });
  }

  @Test
  public void finishEarlyKo() {
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        handler.status = "SUCCESS";
        handler.analysisId = FAKE_ANALYSIS_ID;
        handler.qgStatus = "KO";
        WorkflowRun b = submitPipeline();

        story.j.assertBuildStatus(Result.FAILURE, story.j.waitForCompletion(b));
      }
    });
  }

  @Test
  public void finishEarlyFail() {
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        handler.status = "FAILED";
        WorkflowRun r = submitPipeline();

        story.j.assertBuildStatus(Result.FAILURE, story.j.waitForCompletion(r));
        story.j.assertLogContains("SonarQube task '" + FAKE_TASK_ID + "' status is 'FAILED'", r);
      }
    });
  }

  @Test
  public void waitForQualityGateOk_survive_restart() {
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        handler.status = "PENDING";
        submitPipeline();
        waitForStepToWait();
      }
    });
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        WorkflowJob p = story.j.jenkins.getItemByFullName(JOB_NAME, WorkflowJob.class);
        WorkflowRun b = p.getLastBuild();
        submitWebHook(FAKE_TASK_ID, "SUCCESS", "OK", b);
        story.j.assertBuildStatusSuccess(story.j.waitForCompletion(b));
      }
    });
  }

  @Test
  public void waitForQualityGateOk_recheck_ws_on_restart() {
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        handler.status = "PENDING";
        submitPipeline();
        waitForStepToWait();
      }
    });
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        handler.status = "SUCCESS";
        handler.analysisId = FAKE_ANALYSIS_ID;
        handler.qgStatus = "OK";
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
    waitForStepToWait();

    story.j.postJSON("sonarqube-webhook/", "{\n" +
      "\"taskId\":\"" + taskId + "\",\n" +
      "\"status\":\"" + endTaskStatus + "\",\n" +
      "\"qualityGate\":{\"status\":\"" + qgStatus + "\"}\n" +
      "}");
  }

  private void waitForStepToWait() throws InterruptedException {
    // Wait for the step to register to the webhook listener
    while (SonarQubeWebHook.get().listeners.isEmpty()) {
      Thread.sleep(500);
    }
  }

  private WorkflowRun submitPipeline() throws IOException, InterruptedException, ExecutionException {
    SonarQubeWebHook.get().listeners.clear();
    String serverUrl = "http://localhost:" + port + "/sonarqube";
    File fakeWorkDir = temp.newFolder();
    FileUtils.write(new File(fakeWorkDir, SonarUtils.REPORT_TASK_FILE_NAME), "dashboardUrl=" + serverUrl + "/dashboard\nceTaskId=" + FAKE_TASK_ID);
    story.j.jenkins.getDescriptorByType(SonarGlobalConfiguration.class)
      .setInstallations(
        new SonarInstallation(SONAR_INSTALLATION_NAME, serverUrl, null, null, null, null, null, null, null, null, null, null, null));
    WorkflowJob p = story.j.jenkins.createProject(WorkflowJob.class, JOB_NAME);
    p.setDefinition(new CpsFlowDefinition(
      "node {\n" +
        "  withSonarQubeEnv {\n" +
        "    echo 'Working dir: " + fakeWorkDir.getAbsolutePath() + "'\n" +
        "    sh 'mvn -version'\n" +
        "  }\n" +
        "}\n" +
        "def qg = waitForQualityGate();\n" +
        "if (qg.status != 'OK') {\n" +
        "  error 'QG failure'\n" +
        "}",
      true));
    return p.scheduleBuild2(0).waitForStart();
  }

  static class MyHandler implements HttpHandler {

    String status = "PENDING";
    String analysisId = null;
    String qgStatus = null;

    @Override
    public void handle(HttpExchange t) throws IOException {
      if (t.getRequestURI().getPath().equals("/sonarqube/api/ce/task") && t.getRequestURI().getQuery().equals("id=" + FAKE_TASK_ID)) {
        response(t, 200, "{ task: {\"componentKey\": \"project_1\","
          + "\"componentName\": \"Project One\","
          + (analysisId != null ? ("\"analysisId\": \"" + analysisId + "\",") : "")
          + "\"status\": \"" + status + "\"}}");
      } else if (t.getRequestURI().getPath().equals("/sonarqube/api/qualitygates/project_status") && t.getRequestURI().getQuery().equals("analysisId=" + FAKE_ANALYSIS_ID)) {
        response(t, 200, "{ projectStatus: {\"status\": \"" + qgStatus + "\"}}");
      } else {
        response(t, 404, "not found");
      }
    }

    private void response(HttpExchange t, int code, String body) throws IOException {
      t.sendResponseHeaders(code, body.length());
      OutputStream os = t.getResponseBody();
      os.write(body.getBytes());
      os.close();
    }
  }

}
