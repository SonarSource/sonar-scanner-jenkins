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

import com.google.common.annotations.VisibleForTesting;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.JDK;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.sonar.action.SonarMarkerAction;
import hudson.plugins.sonar.utils.BuilderUtils;
import hudson.plugins.sonar.utils.ExtendedArgumentListBuilder;
import hudson.plugins.sonar.utils.JenkinsRouter;
import hudson.plugins.sonar.utils.Logger;
import hudson.plugins.sonar.utils.SonarUtils;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map.Entry;
import java.util.Properties;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * @since 1.7
 */
public class SonarRunnerBuilder extends Builder {

  /**
   * Identifies {@link SonarInstallation} to be used.
   */
  private String installationName;
  private String project;
  private String properties;
  private String javaOpts;
  private String additionalArguments;

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
   * @deprecated since 2.5
   */
  @Deprecated
  private transient String sonarRunnerName;

  /**
   * Identifies {@link SonarRunnerInstallation} to be used.
   * @since 2.5
   */
  private String sonarScannerName;

  /**
   * Optional task to run
   * @since 2.1
   */
  private String task;

  @DataBoundConstructor
  public SonarRunnerBuilder() {
    // all fields are optional
  }

  /**
   * @deprecated We're moving to using @DataBoundSetter instead and a much leaner @DataBoundConstructor 
   */
  @Deprecated
  public SonarRunnerBuilder(String installationName, String sonarScannerName, String project, String properties, String javaOpts, String jdk, String task,
    String additionalArguments) {
    this.installationName = installationName;
    this.sonarScannerName = sonarScannerName;
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

  @DataBoundSetter
  public void setInstallationName(String installationName) {
    this.installationName = installationName;
  }

  /**
   * @return name of {@link hudson.plugins.sonar.SonarRunnerInstallation}
   */
  public String getSonarScannerName() {
    return Util.fixNull(sonarScannerName);
  }

  @DataBoundSetter
  public void setSonarScannerName(String sonarScannerName) {
    this.sonarScannerName = sonarScannerName;
  }

  /**
   * Gets the JDK that this Sonar builder is configured with, or null.
   */
  @CheckForNull
  public JDK getJdkFromJenkins() {
    return Jenkins.getInstance().getJDK(jdk);
  }

  public String getJdk() {
    return jdk != null && !jdk.isEmpty() ? jdk : "(Inherit From Job)";
  }

  @DataBoundSetter
  public void setJdk(String jdk) {
    this.jdk = jdk;
  }

  /**
   * @return path to a file with properties for project, never <tt>null</tt>
   */
  public String getProject() {
    return Util.fixNull(project);
  }

  @DataBoundSetter
  public void setProject(String project) {
    this.project = project;
  }

  /**
   * @return additional properties, never <tt>null</tt>
   */
  public String getProperties() {
    return Util.fixNull(properties);
  }

  @DataBoundSetter
  public void setProperties(String properties) {
    this.properties = properties;
  }

  /**
   * @return Java options, never <tt>null</tt>
   */
  public String getJavaOpts() {
    return Util.fixNull(javaOpts);
  }

  @DataBoundSetter
  public void setJavaOpts(String javaOpts) {
    this.javaOpts = javaOpts;
  }

  public String getAdditionalArguments() {
    return Util.fixNull(additionalArguments);
  }

  @DataBoundSetter
  public void setAdditionalArguments(String additionalArguments) {
    this.additionalArguments = additionalArguments;
  }

  public SonarInstallation getSonarInstallation() {
    return SonarInstallation.get(getInstallationName());
  }

  public String getTask() {
    return Util.fixNull(task);
  }

  @DataBoundSetter
  public void setTask(String task) {
    this.task = task;
  }

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  public SonarRunnerInstallation getSonarRunnerInstallation() {
    for (SonarRunnerInstallation sri : getDescriptor().getSonarRunnerInstallations()) {
      if (sonarScannerName != null && sonarScannerName.equals(sri.getName())) {
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
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
    FilePath workspace = build.getWorkspace();
    if (workspace == null) {
      throw new AbortException("no workspace for " + build);
    }
    perform(build, workspace, launcher, listener);
    return true;
  }

  private void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
    if (!SonarInstallation.isValid(getInstallationName(), listener)) {
      throw new AbortException("Invalid SonarQube server installation");
    }

    ArgumentListBuilder args = new ArgumentListBuilder();

    EnvVars env = BuilderUtils.getEnvAndBuildVars(run, listener);

    SonarRunnerInstallation sri = getSonarRunnerInstallation();
    if (sri == null) {
      // No idea if the path contains old sonar-runner or new sonar-scanner, so prefer the new one
      args.add(launcher.isUnix() ? "sonar-scanner" : "sonar-scanner.bat");
    } else {
      sri = BuilderUtils.getBuildTool(sri, env, listener, workspace);
      String exe = sri.getExecutable(launcher);
      if (exe == null) {
        Logger.printFailureMessage(listener);
        String msg = Messages.SonarScanner_ExecutableNotFound(sri.getName());
        listener.fatalError(msg);
        throw new AbortException(msg);
      }
      args.add(exe);
    }

    SonarInstallation sonarInst = getSonarInstallation();
    addTaskArgument(args);
    addAdditionalArguments(args, sonarInst);
    ExtendedArgumentListBuilder argsBuilder = new ExtendedArgumentListBuilder(args, launcher.isUnix());
    populateConfiguration(argsBuilder, run, workspace, listener, env, sonarInst);

    // Java
    computeJdkToUse(run, workspace, listener, env);

    // Java options
    env.put("SONAR_SCANNER_OPTS", getJavaOpts());
    // For backward compatibility with old sonar-runner
    env.put("SONAR_RUNNER_OPTS", getJavaOpts());

    long startTime = System.currentTimeMillis();
    int exitCode;
    try {
      exitCode = executeSonarQubeScanner(run, workspace, launcher, listener, args, env);
    } catch (IOException e) {
      handleErrors(listener, sri, startTime, e);
      exitCode = -1;
    }

    // with workflows, we don't have realtime access to build logs, so url might be null
    // if the analyis doesn't succeed, it will also be null
    SonarUtils.addBuildInfoTo(run, workspace, getSonarInstallation().getName());

    if (exitCode != 0) {
      throw new AbortException("SonarQube scanner exited with non-zero code: " + exitCode);
    }
  }

  private void handleErrors(TaskListener listener, @Nullable SonarRunnerInstallation sri, long startTime, IOException e) {
    Logger.printFailureMessage(listener);
    Util.displayIOException(e, listener);

    String errorMessage = Messages.SonarScanner_ExecFailed();
    if (sri == null && (System.currentTimeMillis() - startTime) < 1000 && getDescriptor().getSonarRunnerInstallations() == null) {
      // looks like the user didn't configure any SonarQube Scanner installation
      errorMessage += Messages.SonarScanner_GlobalConfigNeeded();
    }
    e.printStackTrace(listener.fatalError(errorMessage));
  }

  private static int executeSonarQubeScanner(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener, ArgumentListBuilder args, EnvVars env)
    throws IOException, InterruptedException {
    return launcher.launch().cmds(args).envs(env).stdout(listener).pwd(BuilderUtils.getModuleRoot(build, workspace)).join();
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
      Computer computer = workspace.toComputer();
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
  }

  private void addTaskArgument(ArgumentListBuilder args) {
    if (StringUtils.isNotBlank(getTask())) {
      args.add(task);
    }
  }

  @VisibleForTesting
  void populateConfiguration(ExtendedArgumentListBuilder args, Run<?, ?> build, FilePath workspace,
    TaskListener listener, EnvVars env, @Nullable SonarInstallation si) throws IOException, InterruptedException {
    if (si != null) {
      args.append("sonar.jdbc.url", si.getDatabaseUrl());
      args.appendMasked("sonar.jdbc.username", si.getDatabaseLogin());
      args.appendMasked("sonar.jdbc.password", si.getDatabasePassword());
      args.append("sonar.host.url", si.getServerUrl());
      if (StringUtils.isNotBlank(si.getServerAuthenticationToken())) {
        args.appendMasked("sonar.login", si.getServerAuthenticationToken());
      } else if (StringUtils.isNotBlank(si.getSonarLogin())) {
        args.appendMasked("sonar.login", si.getSonarLogin());
        args.appendMasked("sonar.password", si.getSonarPassword());
      }
    }

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
        FilePath projectSettingsFilePath2 = workspace.child(projectSettingsFile);
        if (projectSettingsFilePath2.exists()) {
          // This must be what the user meant. Let it continue.
          projectSettingsFilePath = projectSettingsFilePath2;
        } else {
          // neither file exists. So this now really does look like an error.
          String msg = "Unable to find SonarQube project settings at " + projectSettingsFilePath;
          listener.fatalError(msg);
          throw new AbortException(msg);
        }
      }
      args.append("project.settings", projectSettingsFilePath.getRemote());
    }

    // Additional properties
    Properties p = new Properties();
    p.load(new StringReader(env.expand(getProperties())));
    loadProperties(args, p);

    if (!p.containsKey("sonar.projectBaseDir")) {
      FilePath moduleRoot = BuilderUtils.getModuleRoot(build, workspace);
      args.append("sonar.projectBaseDir", moduleRoot.getRemote());
    }
  }

  private static void loadProperties(ExtendedArgumentListBuilder args, Properties p) {
    for (Entry<Object, Object> entry : p.entrySet()) {
      args.append(entry.getKey().toString(), entry.getValue().toString());
    }
  }

  /**
   * @return JDK to be used with this project.
   */
  private JDK getJdkToUse(@Nullable AbstractProject<?, ?> project) {
    JDK jdkToUse = getJdkFromJenkins();
    if (jdkToUse == null && project != null) {
      jdkToUse = project.getJDK();
    }
    return jdkToUse;
  }

  @Override
  public Action getProjectAction(AbstractProject<?, ?> project) {
    return new SonarMarkerAction();
  }

  protected Object readResolve() {
    // Migrate old field to new field
    if (sonarRunnerName != null) {
      sonarScannerName = sonarRunnerName;
    }
    return this;
  }

  @Extension
  public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
    // Used in jelly configuration for conditional display of the UI
    public static final boolean BEFORE_V2 = JenkinsRouter.BEFORE_V2;

    public String getGlobalToolConfigUrl() {
      return JenkinsRouter.getGlobalToolConfigUrl();
    }

    /**
     * This method is used in UI, so signature and location of this method is important.
     *
     * @return all configured {@link hudson.plugins.sonar.SonarInstallation}
     */
    public SonarInstallation[] getSonarInstallations() {
      return SonarInstallation.all();
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
      return true;
    }

    @Override
    public String getDisplayName() {
      return Messages.SonarScannerBuilder_DisplayName();
    }

    public SonarRunnerInstallation[] getSonarRunnerInstallations() {
      return Jenkins.getInstance().getDescriptorByType(SonarRunnerInstallation.DescriptorImpl.class).getInstallations();
    }

  }

}
