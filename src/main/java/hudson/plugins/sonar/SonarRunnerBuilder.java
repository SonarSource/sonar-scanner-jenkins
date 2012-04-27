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
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

import java.io.IOException;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @since 1.7
 */
public class SonarRunnerBuilder extends Builder {

  private final String installationName;
  private final String project;
  private final String properties;
  private final String javaOpts;

  @DataBoundConstructor
  public SonarRunnerBuilder(String installationName, String project, String properties, String javaOpts) {
    this.installationName = installationName;
    this.javaOpts = javaOpts;
    this.project = project;
    this.properties = properties;
  }

  /**
   * @return name of {@link hudson.plugins.sonar.SonarInstallation}
   */
  public String getInstallationName() {
    return Util.fixNull(installationName);
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

  public SonarInstallation getSonarInstallation(EnvVars envVars) {
    return SonarInstallation.get(envVars.expand(getInstallationName()));
  }

  @Override
  public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
    // TODO kind of copy-paste from SonarPublisher#isSkip
    final String skipLaunchMsg;
    final EnvVars envVars = build.getEnvironment(listener);
    SonarInstallation sonarInstallation = getSonarInstallation(envVars);
    if (sonarInstallation == null) {
      skipLaunchMsg = Messages.SonarPublisher_NoInstallation(getInstallationName(), SonarInstallation.all().length);
    } else if (sonarInstallation.isDisabled()) {
      skipLaunchMsg = Messages.SonarPublisher_InstallDisabled(sonarInstallation.getName());
    } else {
      skipLaunchMsg = null;
    }
    if (skipLaunchMsg != null) {
      listener.getLogger().println(skipLaunchMsg);
      return true;
    }

    SonarRunner sonarRunner = new SonarRunner(build.getProject(), launcher, envVars, build.getWorkspace());
    // Badge should be added only once - SONARPLUGINS-1521
    if (build.getAction(BuildSonarAction.class) == null) {
      build.addAction(new BuildSonarAction());
    }
    return sonarRunner.launch(listener, this) == 0;
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
      return "Invoke Standalone Sonar Analysis";
    }

  }

}
