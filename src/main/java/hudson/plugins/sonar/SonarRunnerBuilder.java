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
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Hudson;
import hudson.model.JDK;
import hudson.plugins.sonar.utils.ExtendedArgumentListBuilder;
import hudson.plugins.sonar.utils.Logger;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * @since 1.7
 */
public class SonarRunnerBuilder extends Builder {

  /**
   * Identifies {@link SonarInstallation} to be used.
   */
  private final String installationName;
  private final String project;
  private final String properties;
  private final String javaOpts;

  /**
   * Identifies {@link JDK} to be used.
   * Null if no explicit configuration is required.
   *
   * <p>
   * Can't store {@link JDK} directly because {@link Jenkins} and {@link hudson.model.Project}
   * are saved independently.
   *
   * @see Jenkins#getJDK(String)
   */
  private String jdk;

  /**
   * Identifies {@link SonarRunnerInstallation} to be used.
   * @since 2.0
   */
  private final String sonarRunnerName;

  /**
   * @deprecated in 2.0
   */
  @Deprecated
  public SonarRunnerBuilder(String installationName, String project, String properties, String javaOpts) {
    this(installationName, null, project, properties, javaOpts, null);
  }

  /**
   * @deprecated in 2.0
   */
  @Deprecated
  public SonarRunnerBuilder(String installationName, String sonarRunnerName, String project, String properties, String javaOpts) {
    this(installationName, sonarRunnerName, project, properties, javaOpts, null);
  }

  @DataBoundConstructor
  public SonarRunnerBuilder(String installationName, String sonarRunnerName, String project, String properties, String javaOpts, String jdk) {
    this.installationName = installationName;
    this.sonarRunnerName = sonarRunnerName;
    this.javaOpts = javaOpts;
    this.project = project;
    this.properties = properties;
    this.jdk = jdk;
  }

  /**
   * @return name of {@link hudson.plugins.sonar.SonarInstallation}
   */
  public String getInstallationName() {
    return Util.fixNull(installationName);
  }

  /**
   * @return name of {@link hudson.plugins.sonar.SonarRunnerInstallation}
   */
  public String getSonarRunnerName() {
    return Util.fixNull(sonarRunnerName);
  }

  /**
   * Gets the JDK that this Sonar builder is configured with, or null.
   */
  public JDK getJDK() {
    return Hudson.getInstance().getJDK(jdk);
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

  public SonarInstallation getSonarInstallation() {
    return SonarInstallation.get(getInstallationName());
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
  public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
    if (!isSonarInstallationValid(getInstallationName(), listener)) {
      return false;
    }

    // Badge should be added only once - SONARPLUGINS-1521
    if (build.getAction(BuildSonarAction.class) == null) {
      build.addAction(new BuildSonarAction());
    }

    ArgumentListBuilder args = new ArgumentListBuilder();

    EnvVars env = build.getEnvironment(listener);
    env.overrideAll(build.getBuildVariables());

    SonarRunnerInstallation sri = getSonarRunnerInstallation();
    if (sri == null) {
      args.add(launcher.isUnix() ? "sonar-runner" : "sonar-runner.bat");
    } else {
      sri = sri.forNode(Computer.currentComputer().getNode(), listener);
      sri = sri.forEnvironment(env);
      String exe = sri.getExecutable(launcher);
      if (exe == null) {
        Logger.printFailureMessage(listener);
        listener.fatalError(Messages.SonarRunner_ExecutableNotFound(sri.getName()));
        return false;
      }
      args.add(exe);
      env.put("SONAR_RUNNER_HOME", sri.getHome());
    }
    ExtendedArgumentListBuilder argsBuilder = new ExtendedArgumentListBuilder(args, launcher.isUnix());
    if (!populateConfiguration(argsBuilder, build, listener, env, getSonarInstallation())) {
      return false;
    }

    // Java
    JDK jdkToUse = getJdkToUse(build.getProject());
    if (jdkToUse != null) {
      Computer computer = Computer.currentComputer();
      // just in case we are not in a build
      if (computer != null) {
        jdkToUse = jdkToUse.forNode(computer.getNode(), listener);
      }
      jdkToUse.buildEnvVars(env);
    }

    // Java options
    env.put("SONAR_RUNNER_OPTS", getJavaOpts());

    long startTime = System.currentTimeMillis();
    try {
      int r = launcher.launch().cmds(args).envs(env).stdout(listener).pwd(build.getModuleRoot()).join();
      return r == 0;
    } catch (IOException e) {
      Logger.printFailureMessage(listener);
      Util.displayIOException(e, listener);

      String errorMessage = Messages.SonarRunner_ExecFailed();
      if (sri == null && (System.currentTimeMillis() - startTime) < 1000 && getDescriptor().getSonarRunnerInstallations() == null) {
        // looks like the user didn't configure any Sonar Runner installation
        errorMessage += Messages.SonarRunner_GlobalConfigNeeded();
      }
      e.printStackTrace(listener.fatalError(errorMessage));
      return false;
    }
  }

  public static boolean isSonarInstallationValid(String sonarInstallationName, BuildListener listener) {
    String failureMsg;
    SonarInstallation sonarInstallation = SonarInstallation.get(sonarInstallationName);
    if (sonarInstallation == null) {
      if (StringUtils.isBlank(sonarInstallationName)) {
        failureMsg = Messages.SonarPublisher_NoInstallation(SonarInstallation.all().length);
      }
      else {
        failureMsg = Messages.SonarPublisher_NoMatchInstallation(sonarInstallationName, SonarInstallation.all().length);
      }
      failureMsg += "\n" + Messages.SonarPublisher_FixInstalltionTip();
    } else if (sonarInstallation.isDisabled()) {
      failureMsg = Messages.SonarPublisher_InstallDisabled(sonarInstallation.getName());
    } else {
      failureMsg = null;
    }
    if (failureMsg != null) {
      Logger.printFailureMessage(listener);
      listener.fatalError(failureMsg);
      return false;
    }
    return true;
  }

  @VisibleForTesting
  boolean populateConfiguration(ExtendedArgumentListBuilder args, AbstractBuild build,
      BuildListener listener, EnvVars env, SonarInstallation si) throws IOException, InterruptedException {
    if (si != null) {
      args.append("sonar.jdbc.driver", si.getDatabaseDriver());
      args.append("sonar.jdbc.url", si.getDatabaseUrl());
      args.appendMasked("sonar.jdbc.username", si.getDatabaseLogin());
      args.appendMasked("sonar.jdbc.password", si.getDatabasePassword());
      args.append("sonar.host.url", si.getServerUrl());
      if (StringUtils.isNotBlank(si.getSonarLogin())) {
        args.appendMasked("sonar.login", si.getSonarLogin());
        args.appendMasked("sonar.password", si.getSonarPassword());
      }
    }

    args.append("sonar.projectBaseDir", build.getModuleRoot().getRemote());

    // Project properties
    if (StringUtils.isNotBlank(getProject())) {
      String projectSettingsFile = env.expand(getProject());
      FilePath projectSettingsFilePath = build.getModuleRoot().child(projectSettingsFile);
      if (!projectSettingsFilePath.exists()) {
        // because of the poor choice of getModuleRoot() with CVS/Subversion, people often get confused
        // with where the build file path is relative to. Now it's too late to change this behavior
        // due to compatibility issue, but at least we can make this less painful by looking for errors
        // and diagnosing it nicely. See HUDSON-1782

        // first check if this appears to be a valid relative path from workspace root
        FilePath projectSettingsFilePath2 = build.getWorkspace().child(projectSettingsFile);
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
    p.load(new ByteArrayInputStream(env.expand(getProperties()).getBytes()));
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
  private JDK getJdkToUse(AbstractProject project) {
    JDK jdkToUse = getJDK();
    if (jdkToUse == null) {
      jdkToUse = project.getJDK();
    }
    return jdkToUse;
  }

  @Extension
  public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

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
      return Messages.SonarRunnerBuilder_DisplayName();
    }

    public SonarRunnerInstallation[] getSonarRunnerInstallations() {
      return Hudson.getInstance().getDescriptorByType(SonarRunnerInstallation.DescriptorImpl.class).getInstallations();
    }

  }

}
