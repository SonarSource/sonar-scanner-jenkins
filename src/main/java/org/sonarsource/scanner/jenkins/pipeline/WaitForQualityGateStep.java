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

import com.google.common.collect.ImmutableSet;
import hudson.AbortException;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.sonar.SonarInstallation;
import hudson.plugins.sonar.action.SonarAnalysisAction;
import hudson.plugins.sonar.client.HttpClient;
import hudson.plugins.sonar.client.WsClient;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.support.actions.PauseAction;
import org.kohsuke.stapler.DataBoundConstructor;

public class WaitForQualityGateStep extends Step implements Serializable {

  private static final Logger LOGGER = Logger.getLogger(WaitForQualityGateStep.class.getName());

  public static class QGStatus implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String status;

    public QGStatus(String status) {
      this.status = status;
    }

    @Whitelisted
    public String getStatus() {
      return status;
    }

  }

  private String taskId;
  private String installationName;
  private String serverUrl;
  private boolean abortPipeline;

  @DataBoundConstructor
  public WaitForQualityGateStep(boolean abortPipeline) {
    super();
    this.abortPipeline = abortPipeline;
  }

  public boolean isAbortPipeline() {
    return abortPipeline;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  public void setInstallationName(String installationName) {
    this.installationName = installationName;
  }

  public void setServerUrl(String serverUrl) {
    this.serverUrl = serverUrl;
  }

  public String getTaskId() {
    return taskId;
  }

  public String getInstallationName() {
    return installationName;
  }

  public String getServerUrl() {
    return serverUrl;
  }

  @Override
  public StepExecution start(StepContext context) throws Exception {
    return new Execution(this, context);
  }

  private static class Execution extends StepExecution implements SonarQubeWebHook.Listener {

    private static final String PLEASE_USE_THE_WITH_SONAR_QUBE_ENV_WRAPPER_TO_RUN_YOUR_ANALYSIS = "Please use the 'withSonarQubeEnv' wrapper to run your analysis.";

    private static final long serialVersionUID = 1L;

    private WaitForQualityGateStep step;

    public Execution(WaitForQualityGateStep step, StepContext context) {
      super(context);
      this.step = step;
    }

    @Override
    public boolean start() throws Exception {
      processStepParameters();
      if (!checkTaskCompleted()) {
        getContext().get(FlowNode.class).addAction(new PauseAction("SonarQube analysis"));
        return false;
      } else {
        return true;
      }
    }

    private void processStepParameters() throws IOException, InterruptedException {
      // Try to read from the Action that may have been previously defined by the SonarBuildWrapper
      List<SonarAnalysisAction> actions = getContext().get(Run.class).getActions(SonarAnalysisAction.class);
      if (actions.isEmpty()) {
        throw new IllegalStateException(
          "No previous SonarQube analysis found on this pipeline execution. " + PLEASE_USE_THE_WITH_SONAR_QUBE_ENV_WRAPPER_TO_RUN_YOUR_ANALYSIS);
      }
      String ceTaskId = null;
      String serverUrl = null;
      String installationName = null;
      // Consider last analysis first
      List<SonarAnalysisAction> reversedActions = new ArrayList<>(actions);
      Collections.reverse(reversedActions);
      for (SonarAnalysisAction a : reversedActions) {
        ceTaskId = a.getCeTaskId();
        if (ceTaskId != null) {
          serverUrl = a.getServerUrl();
          installationName = a.getInstallationName();
          break;
        }
      }
      if (ceTaskId == null || serverUrl == null || installationName == null) {
        throw new IllegalStateException(
          "Unable to guess SonarQube task id and/or SQ server details. "
            + PLEASE_USE_THE_WITH_SONAR_QUBE_ENV_WRAPPER_TO_RUN_YOUR_ANALYSIS);
      }
      step.setTaskId(ceTaskId);
      step.setServerUrl(serverUrl);
      step.setInstallationName(installationName);
    }

    private void log(String msg, Object... args) throws IOException, InterruptedException {
      getContext().get(TaskListener.class).getLogger().printf(msg, args);
      getContext().get(TaskListener.class).getLogger().println();
    }

    private boolean checkTaskCompleted() throws IOException, InterruptedException {
      SonarQubeWebHook.get().addListener(this);
      SonarInstallation inst = SonarInstallation.get(step.getInstallationName());
      if (inst == null) {
        throw new IllegalStateException("Invalid installation name: " + step.getInstallationName());
      }

      log("Checking status of SonarQube task '%s' on server '%s'", step.taskId, step.getInstallationName());
      WsClient wsClient = new WsClient(new HttpClient(), step.getServerUrl(), inst.getServerAuthenticationToken(getContext().get(Run.class)));
      WsClient.CETask ceTask = wsClient.getCETask(step.getTaskId());
      log("SonarQube task '%s' status is '%s'", step.taskId, ceTask.getStatus());
      switch (ceTask.getStatus()) {
        case WsClient.CETask.STATUS_SUCCESS:
          String status = wsClient.requestQualityGateStatus(ceTask.getAnalysisId());
          log("SonarQube task '%s' completed. Quality gate is '%s'", step.taskId, status);
          handleQGStatus(status);
          return true;
        case WsClient.CETask.STATUS_FAILURE:
        case WsClient.CETask.STATUS_CANCELED:
          throw new IllegalStateException("SonarQube analysis '" + step.getTaskId() + "' failed: " + ceTask.getStatus());
        default:
          return false;
      }
    }

    private void handleQGStatus(String status) {
      if (step.isAbortPipeline() && !"OK".equals(status)) {
        getContext().onFailure(new AbortException("Pipeline aborted due to quality gate failure: " + status));
      } else {
        getContext().onSuccess(new QGStatus(status));
      }
    }

    @Override
    public void onResume() {
      SonarQubeWebHook.get().addListener(this);
      try {
        checkTaskCompleted();
      } catch (Exception e) {
        throw new IllegalStateException("Unable to restore step", e);
      }
    }

    @Override
    public void stop(Throwable cause) throws Exception {
      PauseAction.endCurrentPause(getContext().get(FlowNode.class));
      SonarQubeWebHook.get().removeListener(this);
      getContext().onFailure(cause);
    }

    @Override
    public void onTaskCompleted(String taskId, String taskStatus, @Nullable String qgStatus) {
      if (taskId.equals(step.taskId)) {
        try {
          PauseAction.endCurrentPause(getContext().get(FlowNode.class));
          SonarQubeWebHook.get().removeListener(this);
          log("SonarQube task '%s' status is '%s'", step.taskId, taskStatus);
          switch (taskStatus) {
            case WsClient.CETask.STATUS_SUCCESS:
              log("SonarQube task '%s' completed. Quality gate is '%s'", step.taskId, qgStatus);
              handleQGStatus(qgStatus);
              break;
            case WsClient.CETask.STATUS_FAILURE:
            case WsClient.CETask.STATUS_CANCELED:
              getContext().onFailure(new IllegalStateException("SonarQube analysis '" + step.getTaskId() + "' failed: " + taskStatus));
              break;
            default:
              throw new IllegalStateException("Unexpected task status: " + taskStatus);
          }
        } catch (IOException | InterruptedException e) {
          LOGGER.log(Level.WARNING, "Error during WaitForQualityGateStep", e);
        }
      }
    }
  }

  /**
   * Optional: don't log error when pipeline dependencies are not installed
   */
  @Extension(optional = true)
  public static final class DescriptorImpl extends StepDescriptor {

    @Override
    public String getDisplayName() {
      return "Wait for SonarQube analysis to be completed and return quality gate status";
    }

    @Override
    public String getFunctionName() {
      return "waitForQualityGate";
    }

    @Override
    public Set<Class<?>> getRequiredContext() {
      return ImmutableSet.of(FlowNode.class, Run.class, TaskListener.class);
    }

  }

}
