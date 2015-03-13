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
import hudson.*;
import hudson.maven.MavenModuleSet;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.JDK;
import hudson.plugins.sonar.model.TriggersConfig;
import hudson.plugins.sonar.utils.Logger;
import hudson.plugins.sonar.utils.SonarMaven;
import hudson.plugins.sonar.utils.SonarUtils;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.tasks.Maven;
import hudson.tasks.Maven.MavenInstallation;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import jenkins.mvn.GlobalSettingsProvider;
import jenkins.mvn.SettingsProvider;
import jenkins.mvn.DefaultGlobalSettingsProvider;
import jenkins.mvn.DefaultSettingsProvider;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.List;

/**
 * Old fields should be left so that old config data can be read in, but
 * they should be deprecated and transient so that they won't show up in XML
 * when writing back
 */
public class SonarPublisher extends Notifier {

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
   * Sonar installation name.
   */
  private String installationName;

  /**
   * Optional.
   *
   * @since 1.4
   */
  private String branch;

  /**
   * Optional.
   *
   * @since 1.6
   * @deprecated since 2.2 kept for progressive data migration
   */
  @VisibleForTesting
  @Deprecated
  transient String language;

  /**
   * Optional.
   */
  private final String mavenOpts;

  /**
   * Optional.
   */
  private String jobAdditionalProperties;

  /**
   * Triggers. If null, then we should use triggers from {@link SonarInstallation}.
   *
   * @since 1.2
   */
  private final TriggersConfig triggers;

  // =================================================
  // Next fields available only for free-style projects

  /**
   * Null means to reuse job default
   */
  private final String mavenInstallationName;

  /**
   * @since 1.2
   */
  private final String rootPom;

  /**
   * @since 2.1
   */
  private final SettingsProvider settings;

  /**
   * @since 2.1
   */
  private final GlobalSettingsProvider globalSettings;

  /**
   * If true, the build will use its own local Maven repository
   * via "-Dmaven.repo.local=...".
   *
   * @since 2.1
   */
  private final boolean usePrivateRepository;

  @DataBoundConstructor
  public SonarPublisher(String installationName,
    String branch,
    TriggersConfig triggers,
    String jobAdditionalProperties, String mavenOpts,
    String mavenInstallationName, String rootPom, String jdk, SettingsProvider settings, GlobalSettingsProvider globalSettings, boolean usePrivateRepository) {
    this.installationName = installationName;
    this.branch = branch;
    this.jdk = StringUtils.trimToNull(jdk);
    // Triggers
    this.triggers = triggers;
    // Maven
    this.mavenOpts = mavenOpts;
    this.jobAdditionalProperties = jobAdditionalProperties;
    // Non Maven Project
    this.mavenInstallationName = StringUtils.trimToNull(mavenInstallationName);
    this.rootPom = rootPom;
    this.settings = settings != null ? settings : new DefaultSettingsProvider();
    this.globalSettings = globalSettings != null ? globalSettings : new DefaultGlobalSettingsProvider();
    this.usePrivateRepository = usePrivateRepository;
  }

  // compatibility with earlier plugins
  public Object readResolve() {
    if (StringUtils.isNotBlank(this.language)) {
      StringBuilder sb = new StringBuilder();
      sb.append("-Dsonar.language=").append(language);
      if (StringUtils.isNotBlank(this.jobAdditionalProperties)) {
        sb.append(" ").append(this.jobAdditionalProperties);
      }
      this.jobAdditionalProperties = sb.toString();
    }
    return this;
  }

  public String getJdk() {
    return jdk;
  }

  public void setJdk(final String jdk) {
    this.jdk = jdk;
  }

  /**
   * Gets the JDK that this Sonar publisher is configured with, or null.
   */
  public JDK getJDK() {
    return Jenkins.getInstance().getJDK(jdk);
  }

  /**
   * @return name of {@link hudson.plugins.sonar.SonarInstallation}
   */
  public String getInstallationName() {
    return installationName;
  }

  public void setInstallationName(String installationName) {
    this.installationName = installationName;
  }

  /**
   * @return MAVEN_OPTS
   */
  public String getMavenOpts() {
    return mavenOpts;
  }

  /**
   * @return additional Maven options like "-Pprofile" and "-Dname=value"
   */
  public String getJobAdditionalProperties() {
    return StringUtils.trimToEmpty(jobAdditionalProperties);
  }

  public void setJobAdditionalProperties(final String jobAdditionalProperties) {
    this.jobAdditionalProperties = jobAdditionalProperties;
  }

  /**
   * @return true, if we should use triggers from {@link SonarInstallation}
   */
  public boolean isUseGlobalTriggers() {
    return triggers == null;
  }

  public boolean isUseLocalTriggers() {
    return !isUseGlobalTriggers();
  }

  /**
   * See sonar.branch option.
   *
   * @return branch
   * @since 1.4
   */
  public String getBranch() {
    return branch;
  }

  public void setBranch(final String branch) {
    this.branch = branch;
  }

  public String getLanguage() {
    return StringUtils.trimToEmpty(language);
  }

  /**
   * @return triggers configuration
   */
  public TriggersConfig getTriggers() {
    return triggers;
  }

  /**
   * @return name of {@link hudson.tasks.Maven.MavenInstallation}
   */
  public String getMavenInstallationName() {
    return mavenInstallationName;
  }

  /**
   * Root POM. Should be applied only for free-style projects.
   *
   * @return Root POM
   */
  public String getRootPom() {
    return StringUtils.trimToEmpty(rootPom);
  }

  public static boolean isMavenBuilder(AbstractProject<?, ?> currentProject) {
    return currentProject instanceof MavenModuleSet;
  }

  public SonarInstallation getInstallation() {
    return SonarInstallation.get(getInstallationName());
  }

  private boolean isSkip(AbstractBuild<?, ?> build, BuildListener listener, SonarInstallation sonarInstallation) throws IOException, InterruptedException {
    String skipLaunchMsg;
    if (isUseGlobalTriggers()) {
      skipLaunchMsg = sonarInstallation.getTriggers().isSkipSonar(build, listener);
    } else {
      skipLaunchMsg = getTriggers().isSkipSonar(build, listener);
    }
    if (skipLaunchMsg != null) {
      listener.getLogger().println(skipLaunchMsg);
      return true;
    }
    return false;
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
    if (!SonarRunnerBuilder.isSonarInstallationValid(getInstallationName(), listener)) {
      return false;
    }
    SonarInstallation sonarInstallation = getInstallation();

    if (isSkip(build, listener, sonarInstallation)) {
      return true;
    }

    boolean sonarSuccess = executeSonar(build, launcher, listener, sonarInstallation);
    if (!sonarSuccess) {
      // returning false has no effect on the global build status so need to do it manually
      build.setResult(Result.FAILURE);
    } else {
      String url = SonarUtils.extractSonarProjectURLFromLogs(build);
      build.addAction(new BuildSonarAction(url));
    }
    listener.getLogger().println("SonarQube analysis completed: " + build.getResult());
    return sonarSuccess;
  }

  public MavenModuleSet getMavenProject(AbstractBuild<?, ?> build) {
    return (build.getProject() instanceof MavenModuleSet) ? (MavenModuleSet) build.getProject() : null;
  }

  private String getPomName(AbstractBuild<?, ?> build, BuildListener listener) throws IOException, InterruptedException {
    String pomName;
    MavenModuleSet mavenModuleProject = getMavenProject(build);
    if (mavenModuleProject != null) {
      EnvVars envVars = build.getEnvironment(listener);
      pomName = mavenModuleProject.getRootPOM(envVars);
    } else {
      pomName = getRootPom();
    }
    if (StringUtils.isEmpty(pomName)) {
      pomName = "pom.xml";
    }
    return pomName;
  }

  private boolean executeSonar(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, SonarInstallation sonarInstallation) {
    try {
      String pomName = getPomName(build, listener);
      String mavenInstallName = getMavenInstallationName();
      if (isMavenBuilder(build.getProject())) {
        MavenModuleSet mavenModuleSet = getMavenProject(build);
        if (null != mavenModuleSet.getMaven().getName()) {
          mavenInstallName = mavenModuleSet.getMaven().getName();
        }
      }

      // Execute maven
      return SonarMaven.executeMaven(build, launcher, listener, mavenInstallName, pomName, sonarInstallation, this, getJDK(),
        getSettings(), getGlobalSettings(), usesPrivateRepository());
    } catch (IOException e) {
      Logger.printFailureMessage(listener);
      Util.displayIOException(e, listener);
      Logger.LOG.throwing(this.getClass().getName(), "setValues", e);
      e.printStackTrace(listener.fatalError("command execution failed"));
      return false;
    } catch (InterruptedException e) {
      Logger.LOG.throwing(this.getClass().getName(), "executeSonar", e);
      return false;
    } catch (Exception e) {
      Logger.printFailureMessage(listener);
      e.printStackTrace(listener.fatalError("command execution failed"));
      return false;
    }
  }

  @Override
  public Action getProjectAction(AbstractProject<?, ?> project) {
    return new ProjectSonarAction(project);
  }

  @Override
  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.BUILD;
  }

  /**
   * @since 2.1
   */
  public SettingsProvider getSettings() {
    return settings != null ? settings : new DefaultSettingsProvider();
  }

  /**
   * @since 2.1
   */
  public GlobalSettingsProvider getGlobalSettings() {
    return globalSettings != null ? globalSettings : new DefaultGlobalSettingsProvider();
  }

  public boolean usesPrivateRepository() {
    return usePrivateRepository;
  }

  @Extension(ordinal = 1000)
  public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

    @CopyOnWrite
    private volatile SonarInstallation[] installations = new SonarInstallation[0];

    public DescriptorImpl() {
      load();
      save();
    }

    @Override
    public String getDisplayName() {
      return "SonarQube";
    }

    /**
     * @return all configured {@link hudson.plugins.sonar.SonarInstallation}
     */
    public SonarInstallation[] getInstallations() {
      return installations;
    }

    public void setInstallations(SonarInstallation... installations) {
      this.installations = installations;
      save();
    }

    @Override
    public String getHelpFile(String fieldName) {
      if ("globalSettings".equals(fieldName) || "settings".equals(fieldName)) {
        // Reuse help from Maven plugin
        return Jenkins.getInstance().getDescriptorByType(Maven.DescriptorImpl.class).getHelpFile("settings");
      }
      return super.getHelpFile(fieldName);
    }

    /**
     * This method is used in UI, so signature and location of this method is important (see SONARPLUGINS-1337).
     *
     * @return all configured {@link hudson.tasks.Maven.MavenInstallation}
     */
    public MavenInstallation[] getMavenInstallations() {
      return Jenkins.getInstance().getDescriptorByType(Maven.DescriptorImpl.class).getInstallations();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) {
      List<SonarInstallation> list = req.bindJSONToList(SonarInstallation.class, json.get("inst"));
      setInstallations(list.toArray(new SonarInstallation[list.size()]));
      return true;
    }

    public FormValidation doCheckMandatory(@QueryParameter String value) {
      return StringUtils.isBlank(value) ?
        FormValidation.error(Messages.SonarPublisher_MandatoryProperty()) : FormValidation.ok();
    }

    public FormValidation doCheckMandatoryAndNoSpaces(@QueryParameter String value) {
      return (StringUtils.isBlank(value) || value.contains(" ")) ?
        FormValidation.error(Messages.SonarPublisher_MandatoryPropertySpaces()) : FormValidation.ok();
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
      // eventually check if job type of FreeStyleProject.class || MavenModuleSet.class
      return true;
    }
  }
}
