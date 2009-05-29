package hudson.plugins.sonar;

import hudson.CopyOnWrite;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.maven.MavenModuleSet;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.plugins.sonar.template.SimpleTemplate;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Maven;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.tasks.Maven.MavenInstallation;
import hudson.triggers.SCMTrigger.SCMTriggerCause;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class SonarPublisher extends Notifier {
  private final String jobAdditionalProperties;
  private final String installationName;
  private final String mavenInstallationName;
  private final boolean useSonarLight;
  private final String groupId;
  private final String artifactId;
  private final String projectName;
  private final String projectVersion;
  private final String projectDescription;
  private final String javaVersion;
  private final String projectSrcDir;
  private final String mavenOpts;
  private boolean skipOnScm = true;
  private boolean skipIfBuildFails = true;

  @DataBoundConstructor
  public SonarPublisher(String installationName, String jobAdditionalProperties, boolean useSonarLight,
      String groupId, String artifactId, String projectName, String projectVersion, String projectSrcDir, String javaVersion,
      String projectDescription, String mavenOpts, String mavenInstallationName, boolean skipOnScm, boolean skipIfBuildFails) {
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
    this.skipOnScm = skipOnScm;
    this.mavenInstallationName = mavenInstallationName;
    this.skipIfBuildFails = skipIfBuildFails;
  }

  public String getJobAdditionalProperties() {
    return StringUtils.defaultString(jobAdditionalProperties);
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

  public boolean isSkipOnScm() {
    return skipOnScm;
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
    return StringUtils.isBlank(projectVersion) ? "1.0" : projectVersion;
  }

  public String getJavaVersion() {
    return StringUtils.isBlank(javaVersion) ? "1.5" : javaVersion;
  }

  public String getProjectSrcDir() {
    return projectSrcDir;
  }

  public String getProjectDescription() {
    return StringUtils.isBlank(projectDescription) ? "" : projectDescription;
  }
  
  public String getMavenOpts() {
    return mavenOpts;
  }
  
  public static boolean isMavenBuilder(AbstractProject currentProject) {
    return (currentProject instanceof MavenModuleSet);
  }
  
  public List<MavenInstallation> getMavenInstallations() {
    return Arrays.asList(Hudson.getInstance().getDescriptorByType(Maven.DescriptorImpl.class).getInstallations());
  }
  
  public MavenInstallation getMavenInstallation() {
    Maven.DescriptorImpl mavenDescriptor = Hudson.getInstance().getDescriptorByType(Maven.DescriptorImpl.class);
    if (StringUtils.isEmpty(mavenInstallationName) && mavenDescriptor.getInstallations().length > 0) {
      return mavenDescriptor.getInstallations()[0];
    }
    for (MavenInstallation si : getMavenInstallations()) {
      if (StringUtils.equals(mavenInstallationName, si.getName())) {
        return si;
      }
    }
    return null;
  }

  public SonarInstallation getInstallation() {
    DescriptorImpl sonarDescriptor = Hudson.getInstance().getDescriptorByType(DescriptorImpl.class);
    if (StringUtils.isEmpty(installationName) && sonarDescriptor.getInstallations().length > 0) {
      return sonarDescriptor.getInstallations()[0];
    }
    for (SonarInstallation si : sonarDescriptor.getInstallations()) {
      if (StringUtils.equals(installationName, si.getName())) {
        return si;
      }
    }
    return null;
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
    SonarInstallation sonarInstallation = getInstallation();
    String skipLaunchMsg = null;
    if (skipIfBuildFails && build.getResult().isWorseThan(Result.SUCCESS)) {
      skipLaunchMsg = Messages.SonarPublisher_BadBuildStatus(build.getResult().toString());
    } else if (sonarInstallation == null) {
      skipLaunchMsg = Messages.SonarPublisher_NoInstallation(installationName, Hudson.getInstance().getDescriptorByType(Maven.DescriptorImpl.class).getInstallations().length);
    } else if (sonarInstallation.isDisabled()) {
      skipLaunchMsg = Messages.SonarPublisher_InstallDisabled(sonarInstallation.getName());
    } else if (isSkipOnScm() && isSCMTrigger(build)) {
      skipLaunchMsg = Messages.SonarPublisher_SCMBuild();
    }
    if (skipLaunchMsg != null) {
      listener.getLogger().println(skipLaunchMsg);
      return true;
    }
    boolean sonarSuccess = executeSonar(build, launcher, listener, sonarInstallation);
    if (!sonarSuccess) {
      // returning false has no effect on the global build status so need to do it manually
      build.setResult(Result.FAILURE);
    }
    return sonarSuccess;
  }
  
  private boolean isSCMTrigger(AbstractBuild<?,?> build) {
    CauseAction buildCause = build.getAction(CauseAction.class);
    List<Cause> buildCauses = buildCause.getCauses();
    for (Cause cause : buildCauses) {
      if (cause instanceof SCMTriggerCause) {
        return true;
      }
    }
    return false;
  }

  private Maven.MavenInstallation getMavenInstallationForSonar(AbstractBuild<?, ?> build, TaskListener listener) throws IOException, InterruptedException {
    Maven.MavenInstallation mavenInstallation = null;
    if (build.getProject() instanceof Maven.ProjectWithMaven) {
      mavenInstallation = ((Maven.ProjectWithMaven) build.getProject()).inferMavenInstallation();
    }
    if (mavenInstallation == null) {
      mavenInstallation = getMavenInstallation();
    }
    return mavenInstallation != null ? mavenInstallation.forNode(build.getBuiltOn(),listener) : mavenInstallation;
  }
  
  private MavenModuleSet getMavenProject(AbstractBuild build) {
    return (build.getProject() instanceof MavenModuleSet) ? (MavenModuleSet)build.getProject() : null;
  }

  private boolean executeSonar(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener, SonarInstallation sonarInstallation) {
    try {
        Maven.MavenInstallation mavenInstallation = getMavenInstallationForSonar(build,listener);
      FilePath root = build.getProject().getModuleRoot();
      MavenModuleSet mavenModuleProject = getMavenProject(build);
      String pomName = mavenModuleProject != null ? mavenModuleProject.getRootPOM() : "pom.xml";
      if (useSonarLight) {
        generatePomForNonMavenProject(root);
        pomName = "sonar-pom.xml";
      }

      String executable = buildExecName(launcher, mavenInstallation, listener.getLogger());
      String[] command = buildCommand(build, sonarInstallation, executable, pomName);

      EnvVars environmentVars = getMavenEnvironmentVars(build, mavenInstallation, sonarInstallation);
      int r = launcher.launch(command, environmentVars, listener.getLogger(), root).join();
      return r == 0;
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

  private void generatePomForNonMavenProject(FilePath root) throws IOException, InterruptedException {
    SimpleTemplate pomTemplate = new SimpleTemplate("hudson/plugins/sonar/sonar-light-pom.template");
    pomTemplate.setAttribute("groupId", getGroupId());
    pomTemplate.setAttribute("artifactId", getArtifactId());
    pomTemplate.setAttribute("projectName", getProjectName());
    pomTemplate.setAttribute("projectVersion", getProjectVersion());
    pomTemplate.setAttribute("projectSrcDir", getProjectSrcDir());
    pomTemplate.setAttribute("javaVersion", getJavaVersion());
    pomTemplate.setAttribute("projectDescription", getProjectDescription());
    pomTemplate.write(root);
    
  }

  private EnvVars getMavenEnvironmentVars(AbstractBuild<?, ?> build, Maven.MavenInstallation mavenInstallation, SonarInstallation sonarInstallation) throws IOException, InterruptedException {
    EnvVars environmentVars = build.getEnvironment();
    if (mavenInstallation != null) {
      environmentVars.put("M2_HOME", mavenInstallation.getMavenHome());
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

  private static String buildExecName(Launcher launcher, Maven.MavenInstallation mavenInstallation, PrintStream logger) {
    String execName = launcher.isUnix() ? "mvn" : "mvn.bat";
    String separator = launcher.isUnix() ? "/" : "\\";

    String executable = execName;
    if (mavenInstallation != null) {
      String mavenHome = mavenInstallation.getMavenHome();
      executable = mavenHome + separator + "bin" + separator + execName;
    } else {
      logger.println(Messages.SonarPublisher_NoMavenInstallation());
    }
    return executable;
  }

  private String[] buildCommand(AbstractBuild<?, ?> build, SonarInstallation sonarInstallation, String executable, String pomName) {
    ArgumentListBuilder args = new ArgumentListBuilder();
    args.add(executable).add("-e").add("-B")
      .addTokenized("-f " + pomName)
      .addKeyValuePairs("-D", build.getBuildVariables())
      .addTokenized(sonarInstallation.getPluginCallArgs())
      .addTokenized(getJobAdditionalProperties());
    return args.toCommandArray();
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
      return "/plugin/sonar/help.html";
    }

    public SonarInstallation[] getInstallations() {
      return installations;
    }

    @Override
    public boolean configure(StaplerRequest req) {
      installations = req.bindParametersToList(SonarInstallation.class, "sonar.").toArray(new SonarInstallation[0]);
      save();
      return true;
    }

    @Override
    public Notifier newInstance(StaplerRequest req) {
      return req.bindParameters(SonarPublisher.class, "sonar.");
    }

    // web methods
    public FormValidation doCheckMandatory(@QueryParameter String value) {
      if (StringUtils.isBlank(value)) {
        return FormValidation.error(Messages.SonarPublisher_MandatoryProperty());
      }
      return FormValidation.ok();
    }

    public FormValidation doCheckMandatoryAndNoSpaces(@QueryParameter String value) {
      if (StringUtils.isBlank(value) || value.contains(" ")) {
        return FormValidation.error(Messages.SonarPublisher_MandatoryPropertySpaces());
      }
      return FormValidation.ok();
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
      // eventually check if job type of FreeStyleProject.class || MavenModuleSet.class
      return true;
    }
  }
}
