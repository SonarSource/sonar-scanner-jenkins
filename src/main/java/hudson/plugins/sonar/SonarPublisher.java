/*
 * Sonar, entreprise quality control tool.
 * Copyright (C) 2007-2008 Hortis-GRC SA
 * mailto:be_agile HAT hortis DOT ch
 *
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

import hudson.*;
import hudson.maven.AbstractMavenProject;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.maven.ModuleName;
import hudson.model.*;
import hudson.plugins.sonar.model.LightProjectConfig;
import hudson.plugins.sonar.model.ReportsConfig;
import hudson.plugins.sonar.model.TriggersConfig;
import hudson.plugins.sonar.template.SonarPomGenerator;
import hudson.plugins.sonar.utils.MagicNames;
import hudson.plugins.sonar.utils.SonarMaven;
import hudson.tasks.*;
import hudson.tasks.Maven.MavenInstallation;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Old fields are left so that old config data can be read in, but
 * they are deprecated and transient so that they won't show up in XML
 * when writing back
 */
public class SonarPublisher extends Notifier {
  private static final Logger LOG = Logger.getLogger(SonarPublisher.class.getName());

  /**
   * Store a config version, so we're able to migrate config on various
   * functionality upgrades.
   */
  private Integer configVersion;

  /**
   * Sonar installation name.
   */
  private final String installationName;

  /**
   * Optional.
   */
  private final String mavenOpts;

  /**
   * Optional.
   */
  private final String jobAdditionalProperties;

  /**
   * Triggers. If null, then we should use triggers from {@link SonarInstallation}.
   *
   * @since 1.2
   */
  private TriggersConfig triggers;

  @Deprecated
  private transient Boolean scmBuilds;
  @Deprecated
  private transient Boolean timerBuilds;
  @Deprecated
  private transient Boolean snapshotDependencyBuilds;
  @Deprecated
  private transient Boolean skipIfBuildFails;
  @Deprecated
  private transient Boolean skipOnScm; //NOSONAR

  // =================================================
  // Next fields available only for free-style projects

  private String mavenInstallationName;

  /**
   * @since 1.2
   */
  private String rootPom;

  @Deprecated
  private transient Boolean useSonarLight;

  /**
   * If not null, then we should generate pom.xml.
   *
   * @since 1.2
   */
  private LightProjectConfig lightProject;

  @Deprecated
  private transient String groupId;
  @Deprecated
  private transient String artifactId;
  @Deprecated
  private transient String projectName;
  @Deprecated
  private transient String projectVersion;
  @Deprecated
  private transient String projectDescription;
  @Deprecated
  private transient String javaVersion;
  @Deprecated
  private transient String projectSrcDir;
  @Deprecated
  private transient String projectSrcEncoding;
  @Deprecated
  private transient String projectBinDir;
  @Deprecated
  private transient Boolean reuseReports;
  @Deprecated
  private transient String surefireReportsPath;
  @Deprecated
  private transient String coberturaReportPath;
  @Deprecated
  private transient String cloverReportPath;

  public SonarPublisher(String installationName, String jobAdditionalProperties, String mavenOpts) {
    this(installationName, new TriggersConfig(), jobAdditionalProperties, mavenOpts, null, null, null);
  }

  public SonarPublisher(
      String installationName,
      TriggersConfig triggers,
      String jobAdditionalProperties, String mavenOpts
  ) {
    this(installationName, triggers, jobAdditionalProperties, mavenOpts, null, null, null);
  }

  @DataBoundConstructor
  public SonarPublisher(String installationName,
                        TriggersConfig triggers,
                        String jobAdditionalProperties, String mavenOpts,
                        String mavenInstallationName, String rootPom,
                        LightProjectConfig lightProject
  ) {
    super();
    this.configVersion = 1;
    this.installationName = installationName;
    // Triggers
    this.triggers = triggers;
    // Maven
    this.mavenOpts = mavenOpts;
    this.jobAdditionalProperties = jobAdditionalProperties;
    // Non Maven Project
    this.mavenInstallationName = mavenInstallationName;
    this.rootPom = rootPom;
    // Sonar Light
    this.lightProject = lightProject;
  }

  /**
   * Migrate data.
   *
   * @return this
   */
  @SuppressWarnings({"UnusedDeclaration"})
  public Object readResolve() {
    // Default unspecified to v0
    if (configVersion == null) {
      configVersion = 0;
    }
    if (configVersion < 1) {
      // Triggers migration
      if (scmBuilds != null && timerBuilds != null && snapshotDependencyBuilds != null && skipIfBuildFails != null) {
        this.triggers = new TriggersConfig(
            scmBuilds,
            timerBuilds,
            true,
            snapshotDependencyBuilds,
            skipIfBuildFails
        );
      }
      // Project migration
      if (useSonarLight != null && useSonarLight) {
        ReportsConfig reportsConfig = null;
        if (reuseReports != null && reuseReports) {
          reportsConfig = new ReportsConfig(surefireReportsPath, coberturaReportPath, cloverReportPath);
        }
        this.lightProject = new LightProjectConfig(
            groupId,
            artifactId,
            projectName,
            projectVersion,
            projectDescription,
            javaVersion,
            projectSrcDir,
            projectSrcEncoding,
            projectBinDir,
            reportsConfig
        );
      }
      configVersion = 1;
    }
    return this;
  }

  /**
   * @return config version
   */
  public Integer getConfigVersion() {
    return configVersion;
  }

  /**
   * @return name of {@link hudson.plugins.sonar.SonarInstallation}
   */
  public String getInstallationName() {
    return installationName;
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

  /**
   * @return true, if we should generate pom.xml
   */
  public boolean isUseSonarLight() {
    return lightProject != null;
  }

  /**
   * @return configuration for Sonar Light
   */
  public LightProjectConfig getLightProject() {
    return lightProject;
  }

  @SuppressWarnings({"UnusedDeclaration"})
  public static boolean isMavenBuilder(AbstractProject currentProject) {
    return currentProject instanceof MavenModuleSet;
  }

  /**
   * Returns list of configured Maven installations. This method used in UI.
   *
   * @return list of configured Maven installations
   */
  @SuppressWarnings({"UnusedDeclaration"})
  public static List<MavenInstallation> getMavenInstallations() {
    return Arrays.asList(Hudson.getInstance().getDescriptorByType(Maven.DescriptorImpl.class).getInstallations());
  }

  public SonarInstallation getInstallation() {
    DescriptorImpl sonarDescriptor = Hudson.getInstance().getDescriptorByType(DescriptorImpl.class);
    if (StringUtils.isEmpty(getInstallationName()) && sonarDescriptor.getInstallations().length > 0) {
      return sonarDescriptor.getInstallations()[0];
    }
    for (SonarInstallation si : sonarDescriptor.getInstallations()) {
      if (StringUtils.equals(getInstallationName(), si.getName())) {
        return si;
      }
    }
    return null;
  }

  private boolean isSkip(AbstractBuild<?, ?> build, BuildListener listener, SonarInstallation sonarInstallation) {
    final String skipLaunchMsg;
    if (sonarInstallation == null) {
      skipLaunchMsg = Messages.SonarPublisher_NoInstallation(getInstallationName(), Hudson.getInstance().getDescriptorByType(DescriptorImpl.class).getInstallations().length);
    } else if (sonarInstallation.isDisabled()) {
      skipLaunchMsg = Messages.SonarPublisher_InstallDisabled(sonarInstallation.getName());
    } else if (isUseGlobalTriggers()) {
      skipLaunchMsg = sonarInstallation.getTriggers().isSkipSonar(build);
    } else {
      skipLaunchMsg = getTriggers().isSkipSonar(build);
    }
    if (skipLaunchMsg != null) {
      listener.getLogger().println(skipLaunchMsg);
      return true;
    }
    return false;
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
    final SonarInstallation sonarInstallation = getInstallation();
    if (isSkip(build, listener, sonarInstallation)) {
      return true;
    }
    build.addAction(new BuildSonarAction());
    
    boolean sonarSuccess = executeSonar(build, launcher, listener, sonarInstallation);
    if (!sonarSuccess) {
      // returning false has no effect on the global build status so need to do it manually
      build.setResult(Result.FAILURE);
    }
    LOG.info("Sonar build completed: " + build.getResult());
    return sonarSuccess;
  }

  public MavenModuleSet getMavenProject(AbstractBuild build) {
    return (build.getProject() instanceof MavenModuleSet) ? (MavenModuleSet) build.getProject() : null;
  }

  private String getPomName(AbstractBuild<?, ?> build, BuildListener listener) throws IOException, InterruptedException {
    String pomName;
    MavenModuleSet mavenModuleProject = getMavenProject(build);
    if (mavenModuleProject != null) {
      pomName = mavenModuleProject.getRootPOM();
    } else {
      pomName = getRootPom();
    }
    if (StringUtils.isEmpty(pomName)) {
      pomName = "pom.xml";
    }
    // Expand, because pomName can be "${VAR}/pom.xml"
    EnvVars env = build.getEnvironment(listener);
    pomName = env.expand(pomName);
    return pomName;
  }

  private boolean executeSonar(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, SonarInstallation sonarInstallation) {
    try {
      String pomName = getPomName(build, listener);
      FilePath root = build.getModuleRoot();
      if (isUseSonarLight()) {
        LOG.info("Generating " + pomName);
        SonarPomGenerator.generatePomForNonMavenProject(getLightProject(), root, pomName);
      }
      String mavenInstallationName = getMavenInstallationName();
      if (isMavenBuilder( build.getProject() ))
      {
          MavenModuleSet mavenModuleSet = getMavenProject( build );
          if (null != mavenModuleSet.getMaven().getName())
          {
              mavenInstallationName = mavenModuleSet.getMaven().getName();
          }
      }
      
      // Execute maven
      return SonarMaven.executeMaven(build, launcher, listener, mavenInstallationName, pomName, sonarInstallation, this);
    }
    catch (IOException e) {
      Util.displayIOException(e, listener);
      e.printStackTrace(listener.fatalError("command execution failed"));
      return false;
    }
    catch (InterruptedException e) {
      return false;
    } catch (Exception e) {
      e.printStackTrace(listener.fatalError("command execution failed"));
      return false;
    }
  }

  protected String getSonarUrl(AbstractProject<?, ?> project) {
    SonarInstallation sonarInstallation = getInstallation();
    if (sonarInstallation == null) {
      return null;
    }
    String url = sonarInstallation.getServerLink();
    if (project instanceof AbstractMavenProject) {
      // Maven Project
      AbstractMavenProject mavenProject = (AbstractMavenProject) project;
      if (mavenProject.getRootProject() instanceof MavenModuleSet) {
        MavenModuleSet mms = (MavenModuleSet) mavenProject.getRootProject();
        MavenModule rootModule = mms.getRootModule();
        if (rootModule != null) {
          ModuleName moduleName = rootModule.getModuleName();
          url = sonarInstallation.getProjectLink(
              moduleName.groupId,
              moduleName.artifactId
          );
        }
      }
    }
    if (isUseSonarLight()) {
      url = sonarInstallation.getProjectLink(
          lightProject.getGroupId(),
          lightProject.getArtifactId()
      );
    }
    return url;
  }

  @Override
  public Action getProjectAction(AbstractProject<?, ?> project) {
    return new ProjectSonarAction(getSonarUrl(project));
  }

  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.BUILD;
  }

  @Extension
  public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

    @CopyOnWrite
    private volatile SonarInstallation[] installations = new SonarInstallation[0]; //NOSONAR

    public DescriptorImpl() {
      super(SonarPublisher.class);
      load();
    }

    @Override
    public String getDisplayName() {
      return "Sonar";
    }

    @Override
    public String getHelpFile() {
      return MagicNames.PLUGIN_HOME + "/help.html";
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
    public boolean configure(StaplerRequest req, JSONObject json) {
      List<SonarInstallation> list = req.bindJSONToList(SonarInstallation.class, json.get("inst"));
      setInstallations(list.toArray(new SonarInstallation[list.size()]));
      return true;
    }

    @SuppressWarnings({"UnusedDeclaration", "ThrowableResultOfMethodCallIgnored"})
    public FormValidation doCheckMandatory(@QueryParameter String value) {
      return StringUtils.isBlank(value) ?
          FormValidation.error(Messages.SonarPublisher_MandatoryProperty()) : FormValidation.ok();
    }

    @SuppressWarnings({"UnusedDeclaration", "ThrowableResultOfMethodCallIgnored"})
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
