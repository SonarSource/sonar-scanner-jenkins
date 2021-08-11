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

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.google.common.collect.ImmutableSet;
import hudson.AbortException;
import hudson.Extension;
import hudson.Util;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.queue.Tasks;
import hudson.plugins.sonar.SonarInstallation;
import hudson.plugins.sonar.action.SonarAnalysisAction;
import hudson.plugins.sonar.client.HttpClient;
import hudson.plugins.sonar.client.WsClient;
import hudson.plugins.sonar.utils.SonarUtils;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import jenkins.model.Jenkins;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.support.actions.PauseAction;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

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
  private String credentialsId;
  private String webhookSecretId;

  @DataBoundConstructor
  public WaitForQualityGateStep(boolean abortPipeline) {
    super();
    this.abortPipeline = abortPipeline;
  }

  @DataBoundSetter
  public void setWebhookSecretId(String webhookSecretId) {
    this.webhookSecretId = webhookSecretId;
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

  public String getCredentialsId() {
    return credentialsId;
  }

  public String getWebhookSecretId() {
    return webhookSecretId;
  }

  @DataBoundSetter
  public void setCredentialsId(@Nullable String credentialsId) {
    this.credentialsId = Util.fixEmpty(credentialsId);
  }

  @Override
  public StepExecution start(StepContext context) throws Exception {
    return new Execution(this, context);
  }

  private static class Execution extends StepExecution implements Consumer<SonarQubeWebHook.WebhookEvent> {

    private static final String PLEASE_USE_THE_WITH_SONAR_QUBE_ENV_WRAPPER_TO_RUN_YOUR_ANALYSIS = "Please use the 'withSonarQubeEnv' wrapper to run your analysis.";

    private static final long serialVersionUID = 1L;

    private WaitForQualityGateStep step;

    public Execution(WaitForQualityGateStep step, StepContext context) {
      super(context);
      this.step = step;
    }

    @Override
    public boolean start() {
      processStepParameters();

      if (!checkTaskCompleted()) {
        // Check if we received a webhook event after initially checking with the installation.
        SonarQubeWebHook.WebhookEvent webhookEvent = SonarQubeWebHook.get().getWebhookEventForTaskId(step.taskId);
        if (webhookEvent != null) {
          validateWebhookAndCheckQualityGateIfValid(webhookEvent, true);
          return true;
        } else {
          getContextClass(FlowNode.class).addAction(new PauseAction("SonarQube analysis"));
          return false;
        }
      } else {
        return true;
      }
    }

    private void processStepParameters() {
      // Try to read from the Action that may have been previously defined by the SonarBuildWrapper
      List<SonarAnalysisAction> actions = getContextClass(Run.class).getActions(SonarAnalysisAction.class);
      if (actions.isEmpty()) {
        throw new IllegalStateException(
          "No previous SonarQube analysis found on this pipeline execution. " + PLEASE_USE_THE_WITH_SONAR_QUBE_ENV_WRAPPER_TO_RUN_YOUR_ANALYSIS);
      }
      String ceTaskId = null;
      String serverUrl = null;
      String installationName = null;
      String credentialsId = null;
      // Consider last analysis first
      List<SonarAnalysisAction> reversedActions = new ArrayList<>(actions);
      Collections.reverse(reversedActions);
      for (SonarAnalysisAction a : reversedActions) {
        ceTaskId = a.getCeTaskId();
        if (ceTaskId != null) {
          serverUrl = a.getInstallationUrl();
          installationName = a.getInstallationName();
          credentialsId = a.getCredentialsId();
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
      step.setCredentialsId(credentialsId);
      if (step.webhookSecretId == null) {
        step.webhookSecretId = getInstallation().getWebhookSecretId();
      }
    }

    private void log(String msg, Object... args) {
      getContextClass(TaskListener.class).getLogger().printf(msg, args);
      getContextClass(TaskListener.class).getLogger().println();
    }

    private boolean checkTaskCompleted() {
      SonarQubeWebHook.get().addListener(this);

      log("Checking status of SonarQube task '%s' on server '%s'", step.taskId, step.getInstallationName());
      SonarInstallation inst = getInstallation();
      WsClient wsClient = new WsClient(new HttpClient(), step.getServerUrl(), SonarUtils.getAuthenticationToken(getContextClass(Run.class), inst, step.credentialsId));
      WsClient.CETask ceTask = wsClient.getCETask(step.getTaskId());
      return checkQualityGate(ceTask.getStatus(), () -> wsClient.requestQualityGateStatus(ceTask.getAnalysisId()), true);
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
      PauseAction.endCurrentPause(getContextClass(FlowNode.class));
      SonarQubeWebHook.get().removeListener(this);
      getContext().onFailure(cause);
    }

    @Override
    public void accept(SonarQubeWebHook.WebhookEvent event) {
      if (event.getPayload().getTaskId().equals(step.taskId)) {
        try {
          PauseAction.endCurrentPause(getContextClass(FlowNode.class));
          validateWebhookAndCheckQualityGateIfValid(event, false);
        } catch (IOException e) {
          getContext().onFailure(e);
          throw new IllegalStateException(e);
        }
      }
    }

    private void validateWebhookAndCheckQualityGateIfValid(SonarQubeWebHook.WebhookEvent event, boolean onStart) {
      SonarQubeWebHook.get().removeListener(this);
      if (validateWebhook(event)) {
        // only execute the checkQualityGate if the webhook is found to be valid (getContext().onFailure() does not interrupt execution)
        checkQualityGate(event.getPayload().getTaskStatus(), event.getPayload()::getQualityGateStatus, onStart);
      }
    }

    private boolean checkQualityGate(String taskStatus, Supplier<String> qgStatusSupplier, boolean onStart) {
      log("SonarQube task '%s' status is '%s'", step.taskId, taskStatus);
      switch (taskStatus) {
        case WsClient.CETask.STATUS_SUCCESS:
          String qgstatus = qgStatusSupplier.get();
          log("SonarQube task '%s' completed. Quality gate is '%s'", step.taskId, qgstatus);
          handleQGStatus(qgstatus);
          return true;
        case WsClient.CETask.STATUS_FAILURE:
        case WsClient.CETask.STATUS_CANCELED:
          IllegalStateException exception = new IllegalStateException("SonarQube analysis '" + step.getTaskId() + "' failed: " + taskStatus);
          if (onStart) {
            throw exception;
          } else {
            getContext().onFailure(exception);
            return true;
          }
        default:
          if (onStart) {
            return false;
          } else {
            throw new IllegalStateException("Unexpected task status: " + taskStatus);
          }
      }
    }

    private boolean validateWebhook(SonarQubeWebHook.WebhookEvent event) {
      if (step.webhookSecretId != null && !step.webhookSecretId.isEmpty()) {
        StringCredentials webhookSecret = CredentialsProvider.findCredentialById(step.webhookSecretId, StringCredentials.class, getContextClass(Run.class));
        CredentialsProvider.track(getContextClass(Run.class), webhookSecret);
        if (webhookSecret != null) {
          boolean isValidPayload = isValidSignature(event.getReceivedSignature(), event.getPayload().getPayloadAsString(), webhookSecret.getSecret().getPlainText());
          if (!isValidPayload) {
            log("The incoming webhook didn't match the configured webhook secret");
            getContext().onFailure(new AbortException("Pipeline aborted due to failed webhook verification "));
          } else {
            log("The incoming webhook matched the configured webhook secret");
          }
          return isValidPayload;
        } else {
          log("A webhook secret id was configured, but the corresponding credential could not be found");
          getContext().onFailure(new AbortException("Pipeline aborted due to failed webhook verification"));
          return false;
        }
      }
      return true;
    }

    private static boolean isValidSignature(String signature, String payload, String secret) {
      // See Apache commons-codec
      String expectedSignature = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, secret).hmacHex(payload);
      return Objects.equals(expectedSignature, signature);
    }

    private SonarInstallation getInstallation() {
      return Optional.ofNullable(SonarInstallation.get(step.getInstallationName()))
        .orElseThrow(() -> new IllegalStateException("Invalid installation name: " + step.getInstallationName()));
    }

    private <T> T getContextClass(Class<T> contextClass) {
      try {
        return Optional.ofNullable(getContext().get(contextClass))
          .orElseThrow(() -> new IllegalStateException(String.format("Could not get %s from the Jenkins context", contextClass.getName())));
      } catch (IOException | IllegalStateException e) {
        getContext().onFailure(e);
        throw new IllegalStateException(e);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        getContext().onFailure(e);
        throw new IllegalStateException(e);
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

    public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item project,
      @QueryParameter String credentialsId) {
      if (project == null && !Jenkins.getActiveInstance().hasPermission(Jenkins.ADMINISTER) ||
        project != null && !project.hasPermission(Item.EXTENDED_READ)) {
        return new StandardListBoxModel().includeCurrentValue(credentialsId);
      }
      if (project == null) {
        /* Construct a fake project */
        project = new FreeStyleProject(Jenkins.getInstance(), "fake-" + UUID.randomUUID().toString());
      }
      return new StandardListBoxModel()
        .includeEmptyValue()
        .includeMatchingAs(
          project instanceof Queue.Task
            ? Tasks.getAuthenticationOf((Queue.Task) project)
            : ACL.SYSTEM,
          project,
          StringCredentials.class,
          Collections.emptyList(),
          CredentialsMatchers.always())
        .includeCurrentValue(credentialsId);
    }

    public FormValidation doCheckCredentialsId(@AncestorInPath Item project,
      @QueryParameter String value) {
      if (project == null && !Jenkins.getActiveInstance().hasPermission(Jenkins.ADMINISTER) ||
        project != null && !project.hasPermission(Item.EXTENDED_READ)) {
        return FormValidation.ok();
      }

      value = Util.fixEmptyAndTrim(value);
      if (value == null) {
        return FormValidation.ok();
      }

      for (ListBoxModel.Option o : CredentialsProvider
        .listCredentials(StandardUsernameCredentials.class, project, project instanceof Queue.Task
            ? Tasks.getAuthenticationOf((Queue.Task) project)
            : ACL.SYSTEM,
          Collections.emptyList(),
          CredentialsMatchers.always())) {
        if (StringUtils.equals(value, o.value)) {
          return FormValidation.ok();
        }
      }
      // no credentials available, can't check
      return FormValidation.warning("Cannot find any credentials with id " + value);
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
