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
import hudson.maven.MavenModuleSet;
import hudson.model.*;
import hudson.model.Cause.UpstreamCause;
import hudson.model.Cause.UserCause;
import hudson.plugins.sonar.template.SonarPomGenerator;
import hudson.tasks.*;
import hudson.tasks.Maven.MavenInstallation;
import hudson.triggers.SCMTrigger.SCMTriggerCause;
import hudson.triggers.TimerTrigger.TimerTriggerCause;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class SonarPublisher extends Notifier {
  private static final Logger LOG = Logger.getLogger(SonarPublisher.class.getName());

  private final String installationName;

  /**
   * Optional.
   */
  private final String mavenOpts;

  /**
   * Optional.
   */
  private final String jobAdditionalProperties;

  // Triggers

  private final boolean scmBuilds;
  private final boolean timerBuilds;
  private final boolean userBuilds;
  private final boolean snapshotDependencyBuilds;
  private final boolean skipIfBuildFails;
  @Deprecated
  private Boolean skipOnScm;

  // Next properties available only for non-maven projects

  private final String mavenInstallationName;
  private final String rootPom;

  // Next properties available only for Sonar Light

  private final boolean useSonarLight;

  /**
   * Mandatory and no spaces.
   */
  private final String groupId;

  /**
   * Mandatory and no spaces.
   */
  private final String artifactId;

  /**
   * Mandatory.
   */
  private final String projectName;

  /**
   * Optional.
   */
  private final String projectVersion;

  /**
   * Optional.
   */
  private final String projectDescription;

  /**
   * Optional.
   */
  private final String javaVersion;

  /**
   * Mandatory.
   */
  private final String projectSrcDir;

  /**
   * Optional.
   */
  private final String projectSrcEncoding;

  /**
   * Optional.
   */
  private final String projectBinDir;

  private final boolean reuseReports;

  /**
   * Optional.
   */
  private final String surefireReportsPath;

  /**
   * Optional.
   */
  private final String coberturaReportPath;

  /**
   * Optional.
   */
  private final String cloverReportPath;

  @DataBoundConstructor
  public SonarPublisher(String installationName, String jobAdditionalProperties, boolean useSonarLight, String groupId,
                        String artifactId, String projectName, String projectVersion, String projectSrcDir,
                        String javaVersion, String projectDescription,
                        String mavenOpts, String mavenInstallationName, String rootPom,
                        boolean snapshotDependencyBuilds, boolean scmBuilds, boolean timerBuilds, boolean userBuilds,
                        boolean skipIfBuildFails, String projectBinDir, boolean reuseReports,
                        String coberturaReportPath, String surefireReportsPath, String cloverReportPath,
                        String projectSrcEncoding) {
    this.jobAdditionalProperties = jobAdditionalProperties;
    this.installationName = installationName;
    this.useSonarLight = useSonarLight;
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.projectName = projectName;
    this.projectVersion = projectVersion;
    this.javaVersion = javaVersion;
    this.projectSrcDir = projectSrcDir;
    this.projectDescription = projectDescription;
    this.mavenOpts = mavenOpts;
    this.scmBuilds = scmBuilds;
    this.timerBuilds = timerBuilds;
    this.userBuilds = userBuilds;
    this.snapshotDependencyBuilds = snapshotDependencyBuilds;
    this.mavenInstallationName = mavenInstallationName;
    this.skipIfBuildFails = skipIfBuildFails;
    this.projectBinDir = projectBinDir;
    this.reuseReports = reuseReports;
    this.surefireReportsPath = surefireReportsPath;
    this.coberturaReportPath = coberturaReportPath;
    this.cloverReportPath = cloverReportPath;
    this.projectSrcEncoding = projectSrcEncoding;
    this.rootPom = rootPom;
  }

  public String getRootPom() {
    return StringUtils.trimToEmpty(rootPom);
  }

  @Deprecated
  public Boolean getSkipOnScm() {
    return skipOnScm;
  }

  public String getJobAdditionalProperties() {
    return StringUtils.trimToEmpty(jobAdditionalProperties);
  }

  public String getInstallationName() {
    return installationName;
  }

  public boolean isUseSonarLight() {
    return useSonarLight;
  }

  public boolean isSkipIfBuildFails() {
    return skipIfBuildFails;
  }

  public boolean isTimerBuilds() {
    return timerBuilds;
  }

  public boolean isUserBuilds() {
    return userBuilds;
  }

  public boolean isScmBuilds() {
    return scmBuilds;
  }

  public boolean isSnapshotDependencyBuilds() {
    return snapshotDependencyBuilds;
  }

  public String getGroupId() {
    return groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public String getProjectName() {
    return projectName;
  }

  public String getProjectVersion() {
    return StringUtils.trimToEmpty(projectVersion);
  }

  public String getJavaVersion() {
    return StringUtils.trimToEmpty(javaVersion);
  }

  public String getProjectSrcDir() {
    return StringUtils.trimToEmpty(projectSrcDir);
  }

  public String getProjectSrcEncoding() {
    return StringUtils.trimToEmpty(projectSrcEncoding);
  }

  public String getProjectBinDir() {
    return StringUtils.trimToEmpty(projectBinDir);
  }

  public String getProjectDescription() {
    return StringUtils.trimToEmpty(projectDescription);
  }

  public String getMavenOpts() {
    return mavenOpts;
  }

  public boolean isReuseReports() {
    return reuseReports;
  }

  public String getSurefireReportsPath() {
    return StringUtils.trimToEmpty(surefireReportsPath);
  }

  public String getCoberturaReportPath() {
    return StringUtils.trimToEmpty(coberturaReportPath);
  }

  public String getCloverReportPath() {
    return StringUtils.trimToEmpty(cloverReportPath);
  }

  @SuppressWarnings({"UnusedDeclaration"})
  public static boolean isMavenBuilder(AbstractProject currentProject) {
    return currentProject instanceof MavenModuleSet;
  }

  public List<MavenInstallation> getMavenInstallations() {
    return Arrays.asList(Hudson.getInstance().getDescriptorByType(Maven.DescriptorImpl.class).getInstallations());
  }

  public MavenInstallation getMavenInstallation() {
    List<MavenInstallation> installations = getMavenInstallations();
    if (StringUtils.isEmpty(mavenInstallationName) && !installations.isEmpty()) {
      return installations.get(0);
    }
    for (MavenInstallation install : installations) {
      if (StringUtils.equals(mavenInstallationName, install.getName())) {
        return install;
      }
    }
    return null;
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

  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.BUILD;
  }

  protected String isSkipSonar(AbstractBuild<?, ?> build, SonarInstallation sonarInstallation) {
    if (isSkipIfBuildFails() && build.getResult().isWorseThan(Result.SUCCESS)) {
      return Messages.SonarPublisher_BadBuildStatus(build.getResult().toString());
    } else if (sonarInstallation == null) {
      return Messages.SonarPublisher_NoInstallation(getInstallationName(), Hudson.getInstance().getDescriptorByType(Maven.DescriptorImpl.class).getInstallations().length);
    } else if (sonarInstallation.isDisabled()) {
      return Messages.SonarPublisher_InstallDisabled(sonarInstallation.getName());
    } else if (!isScmBuilds() && SonarHelper.isTrigger(build, SCMTriggerCause.class)) {
      return Messages.SonarPublisher_SCMBuild();
    } else if (!isTimerBuilds() && SonarHelper.isTrigger(build, TimerTriggerCause.class)) {
      return Messages.SonarPublisher_TimerBuild();
    } else if (!isUserBuilds() && SonarHelper.isTrigger(build, UserCause.class)) {
      return Messages.SonarPublisher_UserBuild();
    } else if (!isSnapshotDependencyBuilds() && SonarHelper.isTrigger(build, UpstreamCause.class)) {
      return Messages.SonarPublisher_SnapshotDepBuild();
    }
    return null;
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
    SonarInstallation sonarInstallation = getInstallation();
    String skipLaunchMsg = isSkipSonar(build, sonarInstallation);
    if (skipLaunchMsg != null) {
      listener.getLogger().println(skipLaunchMsg);
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

  private Maven.MavenInstallation getMavenInstallationForSonar(AbstractBuild<?, ?> build, TaskListener listener) throws IOException, InterruptedException {
    Maven.MavenInstallation mavenInstallation = null;
    if (build.getProject() instanceof Maven.ProjectWithMaven) {
      mavenInstallation = ((Maven.ProjectWithMaven) build.getProject()).inferMavenInstallation();
    }
    if (mavenInstallation == null) {
      mavenInstallation = getMavenInstallation();
    }
    return mavenInstallation != null ? mavenInstallation.forNode(build.getBuiltOn(), listener) : mavenInstallation;
  }

  protected MavenModuleSet getMavenProject(AbstractBuild build) {
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
      MavenModuleSet mavenModuleProject = getMavenProject(build);
      String pomName = getPomName(build, listener);
      FilePath root = build.getModuleRoot();
      if (isUseSonarLight()) {
        LOG.info("Generating " + pomName);
        SonarPomGenerator.generatePomForNonMavenProject(this, root, pomName);
      }
      // Execute maven
//      MavenInstallation mavenInstallation = getMavenInstallation();
//      return MavenHelper.executeMaven(build, launcher, listener, mavenInstallation.getName(), pomName, sonarInstallation, this);
      return executeMaven(build, launcher, listener, sonarInstallation, mavenModuleProject, root, pomName);
    }
    catch (IOException e) {
      Util.displayIOException(e, listener);
      e.printStackTrace(listener.fatalError("command execution failed"));
      return false;
    }
    catch (InterruptedException e) {
      return false;
    }
  }

  /**
   * @deprecated since 1.2, use {@link MavenHelper}
   */
  @Deprecated
  private boolean executeMaven(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, SonarInstallation sonarInstallation, MavenModuleSet mavenModuleProject, FilePath root, String pomName) throws IOException, InterruptedException {
    Maven.MavenInstallation mavenInstallation = getMavenInstallationForSonar(build, listener);
    String executable = buildExecName(launcher, mavenInstallation, listener.getLogger());

    Launcher.ProcStarter starter = launcher.launch();
    starter.cmds(buildCommand(launcher, listener, build, sonarInstallation, executable, pomName, mavenModuleProject));
    starter.envs(getMavenEnvironmentVars(listener, build, mavenInstallation));
    starter.pwd(root);
    starter.stderr(listener.getLogger());
    starter.stdout(listener.getLogger());
    return starter.join() == 0;
  }

  /**
   * @deprecated since 1.2, use {@link MavenHelper}
   */
  @Deprecated
  private EnvVars getMavenEnvironmentVars(BuildListener listener, AbstractBuild<?, ?> build, Maven.MavenInstallation mavenInstallation) throws IOException, InterruptedException {
    EnvVars environmentVars = build.getEnvironment(listener);
    if (mavenInstallation != null) {
      environmentVars.put("M2_HOME", mavenInstallation.getHome());
    }
    String envMavenOpts = getMavenOpts();
    MavenModuleSet mavenModuleProject = getMavenProject(build);
    if (StringUtils.isEmpty(envMavenOpts) && mavenModuleProject != null && StringUtils.isNotEmpty(mavenModuleProject.getMavenOpts())) {
      envMavenOpts = mavenModuleProject.getMavenOpts();
    }
    if (StringUtils.isNotEmpty(envMavenOpts)) {
      environmentVars.put("MAVEN_OPTS", envMavenOpts);
    }
    return environmentVars;
  }

  /**
   * @deprecated since 1.2, use {@link MavenHelper}
   */
  @Deprecated
  private String buildExecName(Launcher launcher, Maven.MavenInstallation mavenInstallation, PrintStream logger) {
    String execName = launcher.isUnix() ? "mvn" : "mvn.bat";
    String separator = launcher.isUnix() ? "/" : "\\";

    String executable = execName;
    if (mavenInstallation != null) {
      String mavenHome = mavenInstallation.getHome();
      executable = mavenHome + separator + "bin" + separator + execName;
    } else {
      logger.println(Messages.SonarPublisher_NoMavenInstallation());
    }
    return executable;
  }

  /**
   * @deprecated since 1.2, use {@link MavenHelper}
   */
  @Deprecated
  protected ArgumentListBuilder buildCommand(Launcher launcher, BuildListener listener, AbstractBuild<?, ?> build, SonarInstallation sonarInstallation, String executable, String pomName, MavenModuleSet mms) throws IOException, InterruptedException {
    EnvVars envVars = build.getEnvironment(listener);
    ArgumentListBuilder args = new ArgumentListBuilder();
    args.add(executable);
    // Force the use of an alternate POM file,
    // don't use addTokenized - see bug SONARPLUGINS-263 (pom with spaces)
    args.add("-f").add(pomName);
    // Define a system properties
    args.addKeyValuePairs("-D", build.getBuildVariables());
    SonarHelper.addTokenizedAndQuoted(launcher.isUnix(), args, sonarInstallation.getPluginCallArgs(envVars));
    SonarHelper.addTokenizedAndQuoted(launcher.isUnix(), args, envVars.expand(sonarInstallation.getAdditionalProperties()));
    SonarHelper.addTokenizedAndQuoted(launcher.isUnix(), args, envVars.expand(getJobAdditionalProperties()));
    if (mms != null && mms.usesPrivateRepository()) {
      args.add("-Dmaven.repo.local=" + build.getWorkspace().child(".repository").getRemote());
    }
    // Produce execution error messages
    args.add("-e");
    // Run in non-interactive (batch) mode
    args.add("-B");
    // Goal
    args.add("sonar:sonar");
    LOG.info("Sonar build command: " + args.toStringWithQuote());
    return args;
  }

  @Override
  public Action getProjectAction(AbstractProject<?, ?> project) {
    return new ProjectSonarAction(project);
  }

  @Extension
  public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

    @CopyOnWrite
    private volatile SonarInstallation[] installations = new SonarInstallation[0];

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

    public SonarInstallation[] getInstallations() {
      return installations;
    }

    public void setInstallations(SonarInstallation... installations) {
      this.installations = installations;
      save();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) {
      List<SonarInstallation> list = req.bindParametersToList(SonarInstallation.class, "sonar.");
      installations = list.toArray(new SonarInstallation[list.size()]);
      save();
      return true;
    }

    @Override
    public Notifier newInstance(StaplerRequest req, JSONObject json) {
      return req.bindParameters(SonarPublisher.class, "sonar.");
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
