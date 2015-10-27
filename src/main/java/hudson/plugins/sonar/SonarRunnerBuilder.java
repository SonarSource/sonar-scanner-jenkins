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
/*
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package hudson.plugins.sonar;

import hudson.plugins.sonar.utils.BuilderUtils;

import com.google.common.annotations.VisibleForTesting;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.JDK;
import hudson.model.Run;
import hudson.plugins.sonar.action.BuildSonarAction;
import hudson.plugins.sonar.action.ProjectSonarAction;
import hudson.plugins.sonar.action.UrlSonarAction;
import hudson.plugins.sonar.utils.ExtendedArgumentListBuilder;
import hudson.plugins.sonar.utils.Logger;
import hudson.plugins.sonar.utils.SonarUtils;
import hudson.scm.SCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import jenkins.triggers.SCMTriggerItem;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nullable;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * @since 1.7
 */
public class SonarRunnerBuilder extends Builder implements SimpleBuildStep {

  /**
   * Identifies {@link SonarInstallation} to be used.
   */
  private String installationName;
  private final String project;
  private final String properties;
  private final String javaOpts;
  private final String additionalArguments;

  /**
   * Identifies {@link JDK} to be used.
   * Null if no explicit configuration is required.
   *
   * <p>
   * Can't store {@link JDK} directly because {@link jenkins.model.Jenkins} and {@link hudson.model.Project}
   * are saved independently.
   *
   * @see jenkins.model.Jenkins#getJDK(String)
   */
  private String jdk;

  /**
   * Identifies {@link SonarRunnerInstallation} to be used.
   * @since 2.0
   */
  private String sonarRunnerName;

  /**
   * Optional task to run
   * @since 2.1
   */
  private final String task;

  @DataBoundConstructor
  public SonarRunnerBuilder(String installationName, String sonarRunnerName, String project, String properties, String javaOpts, String jdk, String task,
    String additionalArguments) {
    this.installationName = installationName;
    this.sonarRunnerName = sonarRunnerName;
    this.javaOpts = javaOpts;
    this.project = project;
    this.properties = properties;
    this.jdk = jdk;
    this.task = task;
    this.additionalArguments = additionalArguments;
  }

  /**
   * @return name of {@link hudson.plugins.sonar.SonarInstallation}
   */
  public String getInstallationName() {
    return Util.fixNull(installationName);
  }

  public void setInstallationName(String installationName) {
    this.installationName = installationName;
  }

  /**
   * @return name of {@link hudson.plugins.sonar.SonarRunnerInstallation}
   */
  public String getSonarRunnerName() {
    return Util.fixNull(sonarRunnerName);
  }

  public void setSonarRunnerName(String sonarRunnerName) {
    this.sonarRunnerName = sonarRunnerName;
  }

  /**
   * Gets the JDK that this Sonar builder is configured with, or null.
   */
  public JDK getJDK() {
    return Jenkins.getInstance().getJDK(jdk);
  }

  public String getJdkName() {
    return jdk;
  }

  public void setJdkName(String jdk) {
    this.jdk = jdk;
  }

  /**
   * @return path to a file with properties for project, never <tt>null</tt>
   */
  public String getProject() {
    return Util.fixNull(project);
  }

  /**
   * @return additional properties, never <tt>null</tt>
   */
  public String getProperties() {
    return Util.fixNull(properties);
  }

  /**
   * @return Java options, never <tt>null</tt>
   */
  public String getJavaOpts() {
    return Util.fixNull(javaOpts);
  }

  public String getAdditionalArguments() {
    return Util.fixNull(additionalArguments);
  }

  public SonarInstallation getSonarInstallation() {
    return SonarInstallation.get(getInstallationName());
  }

  public String getTask() {
    return task;
  }

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  public SonarRunnerInstallation getSonarRunnerInstallation() {
    for (SonarRunnerInstallation sri : getDescriptor().getSonarRunnerInstallations()) {
      if (sonarRunnerName != null && sonarRunnerName.equals(sri.getName())) {
        return sri;
      }
    }
    // If no installation match then take the first one
    if (getDescriptor().getSonarRunnerInstallations().length > 0) {
      return getDescriptor().getSonarRunnerInstallations()[0];
    }
    return null;
  }

  @Override
  public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
    performInternal(run, workspace, launcher, listener);
  }

  private boolean performInternal(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
    if (!SonarInstallation.isValid(getInstallationName(), listener)) {
      return false;
    }

    ArgumentListBuilder args = new ArgumentListBuilder();

    EnvVars env = BuilderUtils.getEnvAndBuildVars(run, listener);

    SonarRunnerInstallation sri = getSonarRunnerInstallation();
    if (sri == null) {
      args.add(launcher.isUnix() ? "sonar-runner" : "sonar-runner.bat");
    } else {
      sri = BuilderUtils.getBuildTool(sri, env, listener);
      String exe = sri.getExecutable(launcher);
      if (exe == null) {
        Logger.printFailureMessage(listener);
        listener.fatalError(Messages.SonarRunner_ExecutableNotFound(sri.getName()));
        return false;
      }
      args.add(exe);
      env.put("SONAR_RUNNER_HOME", sri.getHome());
    }

    SonarInstallation sonarInst = getSonarInstallation();
    addTaskArgument(args);
    addAdditionalArguments(args, sonarInst);
    ExtendedArgumentListBuilder argsBuilder = new ExtendedArgumentListBuilder(args, launcher.isUnix());
    if (!populateConfiguration(argsBuilder, run, workspace, listener, env, sonarInst)) {
      return false;
    }

    // Java
    computeJdkToUse(run, workspace, listener, env);

    // Java options
    env.put("SONAR_RUNNER_OPTS", getJavaOpts());

    long startTime = System.currentTimeMillis();
    int r;
    try {
      r = executeSonarRunner(run, workspace, launcher, listener, args, env);
    } catch (IOException e) {
      handleErrors(run, listener, sri, startTime, e);
      r = -1;
    }
    return r == 0;
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
    return performInternal(build, build.getWorkspace(), launcher, listener);
  }

  private void handleErrors(Run<?, ?> build, TaskListener listener, SonarRunnerInstallation sri, long startTime, IOException e) {
    Logger.printFailureMessage(listener);
    Util.displayIOException(e, listener);

    String errorMessage = Messages.SonarRunner_ExecFailed();
    if (sri == null && (System.currentTimeMillis() - startTime) < 1000 && getDescriptor().getSonarRunnerInstallations() == null) {
      // looks like the user didn't configure any SonarQube Runner installation
      errorMessage += Messages.SonarRunner_GlobalConfigNeeded();
    }
    e.printStackTrace(listener.fatalError(errorMessage));

    try {
      SonarUtils.addUrlActionTo(build);
    } catch (IOException e1) {
      e1.printStackTrace(listener.fatalError(errorMessage));
    }

    // Badge should be added only once - SONARPLUGINS-1521
    if (build.getAction(BuildSonarAction.class) == null) {
      build.addAction(new BuildSonarAction());
    }
  }

  private int executeSonarRunner(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener, ArgumentListBuilder args, EnvVars env) throws IOException,
    InterruptedException {
    int r = launcher.launch().cmds(args).envs(env).stdout(listener).pwd(BuilderUtils.getModuleRoot(build, workspace)).join();
    UrlSonarAction urlAction = SonarUtils.addUrlActionTo(build);

    // with workflows, we don't have realtime access to build logs, so url might be null
    String url = urlAction != null ? urlAction.getSonarUrl() : null;

    if (build.getAction(BuildSonarAction.class) == null && r == 0) {
      build.addAction(new BuildSonarAction(url));
    }
    return r;
  }

  private static AbstractProject<?, ?> getProject(Run<?, ?> run) {
    AbstractProject<?, ?> project = null;
    if (run instanceof AbstractBuild) {
      AbstractBuild<?, ?> build = (AbstractBuild<?, ?>) run;
      project = build.getProject();
    }
    return project;
  }

  private void computeJdkToUse(Run<?, ?> build, FilePath workspace, TaskListener listener, EnvVars env) throws IOException, InterruptedException {
    JDK jdkToUse = getJdkToUse(getProject(build));
    if (jdkToUse != null) {
      Computer computer = Computer.currentComputer();
      // just in case we are not in a build
      if (computer != null) {
        jdkToUse = jdkToUse.forNode(computer.getNode(), listener);
      }
      jdkToUse.buildEnvVars(env);
    }
  }

  @VisibleForTesting
  void addAdditionalArguments(ArgumentListBuilder args, SonarInstallation inst) {
    args.addTokenized(inst.getAdditionalProperties());
    args.add(inst.getAdditionalAnalysisPropertiesUnix());
    args.addTokenized(additionalArguments);

    if (!args.toList().contains("-e")) {
      args.add("-e");
    }
  }

  private void addTaskArgument(ArgumentListBuilder args) {
    if (StringUtils.isNotBlank(getTask())) {
      args.add(task);
    }
  }

  @VisibleForTesting
  boolean populateConfiguration(ExtendedArgumentListBuilder args, Run<?, ?> build, FilePath workspace,
    TaskListener listener, EnvVars env, SonarInstallation si) throws IOException, InterruptedException {
    if (si != null) {
      args.append("sonar.jdbc.url", si.getDatabaseUrl());
      args.appendMasked("sonar.jdbc.username", si.getDatabaseLogin());
      args.appendMasked("sonar.jdbc.password", si.getDatabasePassword());
      args.append("sonar.host.url", si.getServerUrl());
      if (StringUtils.isNotBlank(si.getSonarLogin())) {
        args.appendMasked("sonar.login", si.getSonarLogin());
        args.appendMasked("sonar.password", si.getSonarPassword());
      }
    }

    FilePath moduleRoot = BuilderUtils.getModuleRoot(build, workspace);
    args.append("sonar.projectBaseDir", moduleRoot.getRemote());

    // Project properties
    if (StringUtils.isNotBlank(getProject())) {
      String projectSettingsFile = env.expand(getProject());
      FilePath projectSettingsFilePath = BuilderUtils.getModuleRoot(build, workspace).child(projectSettingsFile);
      if (!projectSettingsFilePath.exists()) {
        // because of the poor choice of getModuleRoot() with CVS/Subversion, people often get confused
        // with where the build file path is relative to. Now it's too late to change this behavior
        // due to compatibility issue, but at least we can make this less painful by looking for errors
        // and diagnosing it nicely. See HUDSON-1782

        // first check if this appears to be a valid relative path from workspace root
        if (workspace == null) {
          listener.fatalError("Project workspace is null");
          return false;
        }
        FilePath projectSettingsFilePath2 = workspace.child(projectSettingsFile);
        if (projectSettingsFilePath2.exists()) {
          // This must be what the user meant. Let it continue.
          projectSettingsFilePath = projectSettingsFilePath2;
        } else {
          // neither file exists. So this now really does look like an error.
          listener.fatalError("Unable to find Sonar project settings at " + projectSettingsFilePath);
          return false;
        }
      }
      args.append("project.settings", projectSettingsFilePath.getRemote());
    }

    // Additional properties
    Properties p = new Properties();
    p.load(new StringReader(env.expand(getProperties())));
    loadProperties(args, p);

    return true;
  }

  private void loadProperties(ExtendedArgumentListBuilder args, Properties p) {
    for (Entry<Object, Object> entry : p.entrySet()) {
      args.append(entry.getKey().toString(), entry.getValue().toString());
    }
  }

  /**
   * @return JDK to be used with this project.
   */
  private JDK getJdkToUse(@Nullable AbstractProject<?, ?> project) {
    JDK jdkToUse = getJDK();
    if (jdkToUse == null && project != null) {
      jdkToUse = project.getJDK();
    }
    return jdkToUse;
  }

  @Override
  public Action getProjectAction(AbstractProject<?, ?> project) {
    return new ProjectSonarAction(project);
  }

  @Extension
  public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

    /**
     * This method is used in UI, so signature and location of this method is important.
     *
     * @return all configured {@link hudson.plugins.sonar.SonarInstallation}
     */
    public SonarInstallation[] getSonarInstallations() {
      return SonarInstallation.enabled();
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
      return true;
    }

    @Override
    public String getDisplayName() {
      return Messages.SonarRunnerBuilder_DisplayName();
    }

    public SonarRunnerInstallation[] getSonarRunnerInstallations() {
      return Jenkins.getInstance().getDescriptorByType(SonarRunnerInstallation.DescriptorImpl.class).getInstallations();
    }

  }

}
