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

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.Hudson;
import hudson.model.JDK;
import hudson.model.Node;
import hudson.model.Project;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
   * Can't store {@link JDK} directly because {@link Jenkins} and {@link Project}
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

  @Deprecated
  public SonarRunnerBuilder(String installationName, String project, String properties, String javaOpts) {
    this(installationName, null, project, properties, javaOpts, null);
  }

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
    for (SonarRunnerInstallation i : getDescriptor().getSonarRunnerInstallations()) {
      if (sonarRunnerName != null && sonarRunnerName.equals(i.getName()))
        return i;
    }
    //If no installation match then take the first one
    if (getDescriptor().getSonarRunnerInstallations().length > 0) {
      return getDescriptor().getSonarRunnerInstallations()[0];
    }
    return null;
  }

  @Override
  public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
    // TODO copy-paste from SonarPublisher#perform
    String failureMsg;
    SonarInstallation sonarInstallation = getSonarInstallation();
    if (sonarInstallation == null) {
      if (StringUtils.isBlank(getInstallationName())) {
        failureMsg = Messages.SonarPublisher_NoInstallation(SonarInstallation.all().length);
      }
      else {
        failureMsg = Messages.SonarPublisher_NoMatchInstallation(getInstallationName(), SonarInstallation.all().length);
      }
      failureMsg += "\n" + Messages.SonarPublisher_FixInstalltionTip();
    } else if (sonarInstallation.isDisabled()) {
      failureMsg = Messages.SonarPublisher_InstallDisabled(sonarInstallation.getName());
    } else {
      failureMsg = null;
    }
    if (failureMsg != null) {
      listener.getLogger().println(failureMsg);
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
    if(sri==null) {
      args.add(launcher.isUnix() ? "sonar-runner" : "sonar-runner.bat");
    } else {
      sri = sri.forNode(Computer.currentComputer().getNode(), listener);
      sri = sri.forEnvironment(env);
      String exe = sri.getExecutable(launcher);
      if (exe == null) {
        listener.fatalError(Messages.SonarRunner_ExecutableNotFound(sri.getName()));
        return false;
      }
      args.add(exe);
      env.put("SONAR_RUNNER_HOME", sri.getHome());
    }
    populateConfiguration(args, build.getWorkspace().getRemote(), env);

    // Java
    JDK jdk = getJdkToUse(build.getProject());
    if (jdk != null) {
      Computer computer = Computer.currentComputer();
      if (computer != null) { // just in case were not in a build
        jdk = jdk.forNode(computer.getNode(), listener);
      }
      jdk.buildEnvVars(env);
    }

    // Java options
    env.put("SONAR_RUNNER_OPTS", getJavaOpts());

    long startTime = System.currentTimeMillis();
    try {
        int r = launcher.launch().cmds(args).envs(env).stdout(listener).pwd(build.getWorkspace()).join();
        return r==0;
    } catch (IOException e) {
        Util.displayIOException(e,listener);

        String errorMessage = Messages.SonarRunner_ExecFailed();
        if(sri==null && (System.currentTimeMillis()-startTime)<1000) {
            if(getDescriptor().getSonarRunnerInstallations()==null)
                // looks like the user didn't configure any Sonar Runner installation
                errorMessage += Messages.SonarRunner_GlobalConfigNeeded();
        }
        e.printStackTrace( listener.fatalError(errorMessage) );
        return false;
    }
  }

  private void populateConfiguration(ArgumentListBuilder args, String projectBaseDir, EnvVars env) throws IOException {
    // Server properties
    SonarInstallation si = getSonarInstallation();
    if (si != null) {
      appendArg(args, "sonar.jdbc.driver", si.getDatabaseDriver());
      appendArg(args, "sonar.jdbc.url", si.getDatabaseUrl()); // TODO can be masked
      appendMaskedArg(args, "sonar.jdbc.username", si.getDatabaseLogin());
      appendMaskedArg(args, "sonar.jdbc.password", si.getDatabasePassword());
      appendArg(args, "sonar.host.url", si.getServerUrl());
    }

    appendArg(args, "sonar.projectBaseDir", projectBaseDir);

    // Project properties
    if (StringUtils.isNotBlank(getProject())) {
      File projectSettings = new File(getProject());
      Properties p = toProperties(projectSettings);
      loadProperties(args, p);
    }

    // Additional properties
    Properties p = new Properties();
    p.load(new ByteArrayInputStream(env.expand(getProperties()).getBytes()));
    loadProperties(args, p);
  }

  private void loadProperties(ArgumentListBuilder args, Properties p) {
    for (Entry<Object, Object> entry : p.entrySet()) {
      appendArg(args, entry.getKey().toString(), entry.getValue().toString());
    }
  }

  public void appendArg(ArgumentListBuilder args, String name, String value) {
    value = StringUtils.trimToEmpty(value);
    if (StringUtils.isNotEmpty(value)) {
      args.add("-D" + name + "=" + value);
    }
  }

  public void appendMaskedArg(ArgumentListBuilder args, String name, String value) {
    value = StringUtils.trimToEmpty(value);
    if (StringUtils.isNotEmpty(value)) {
      args.addMasked("-D" + name + "=" + value);
    }
  }

  // TODO Duplicated from Sonar Runner Main
  private Properties toProperties(File file) {
    InputStream in = null;
    Properties properties = new Properties();
    try {
      in = new FileInputStream(file);
      properties.load(in);
      return properties;

    } catch (Exception e) {
      throw new IllegalStateException("Fail to load file: " + file.getAbsolutePath(), e);

    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  /**
   * @return JDK to be used with this project.
   */
  private JDK getJdkToUse(AbstractProject project) {
    JDK jdk = getJDK();
    if (jdk == null) {
      jdk = project.getJDK();
    }
    return jdk;
  }

  /**
   * @return the current {@link Node} on which we are building
   */
  private Node getCurrentNode() {
    return Executor.currentExecutor().getOwner().getNode();
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
