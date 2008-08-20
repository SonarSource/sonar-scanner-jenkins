package hudson.plugins.sonar;

import hudson.*;
import hudson.model.*;
import hudson.tasks.Maven;
import hudson.tasks.Publisher;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormFieldValidator;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.*;

import javax.servlet.ServletException;
import java.io.*;
import java.util.Map;

public class SonarPublisher extends Publisher {
  private final String jobAdditionalProperties;
  private final String installationName;

  @DataBoundConstructor
  public SonarPublisher(String installationName, String jobAdditionalProperties) {
    this.jobAdditionalProperties = jobAdditionalProperties;
    this.installationName = installationName;
  }

  public String getJobAdditionalProperties() {
    return StringUtils.defaultString(jobAdditionalProperties);
  }

  public String getInstallationName() {
    return installationName;
  }

  public SonarInstallation getInstallation() {
    if ("".equals(StringUtils.defaultString(installationName)) && DESCRIPTOR.getInstallations().length > 0) {
      return DESCRIPTOR.getInstallations()[0];
    }

    for (SonarInstallation si : DESCRIPTOR.getInstallations()) {
      if (installationName != null && si.getName().equals(installationName)) {
        return si;
      }
    }

    return null;
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
    SonarInstallation sonarInstallation = getInstallation();
    if (sonarInstallation == null) {
      listener.getLogger().println("No Sonar installation on this job. " + installationName + ',' + DESCRIPTOR.getInstallations().length);
      return true;
    }
    if (sonarInstallation.isDisabled()) {
      listener.getLogger().println("Sonar is disabled (version " + sonarInstallation.getName() + "). See Hudson global configuration.");
      return true;
    }
    Maven.MavenInstallation mavenInstallation = inferMavenInstallation(build, listener.getLogger());
    return executeSonar(build, launcher, listener, sonarInstallation, mavenInstallation);
  }

  public Maven.MavenInstallation inferMavenInstallation(AbstractBuild<?, ?> build, PrintStream logger) {
    Maven.MavenInstallation mavenInstallation = null;
    AbstractProject<?, ?> project = build.getProject();
    if (project instanceof Maven.ProjectWithMaven) {
      mavenInstallation = ((Maven.ProjectWithMaven) project).inferMavenInstallation();
    }
    if (mavenInstallation == null && Maven.DESCRIPTOR.getInstallations().length > 0) {
      mavenInstallation = Maven.DESCRIPTOR.getInstallations()[0];
      if (mavenInstallation != null) {
        logger.println("No Maven installation configured for this job. Using the first one configured in Hudson...");
      }
    }
    if (mavenInstallation == null) {
      logger.println("No Maven installation found. We'll try to call the 'mvn' executable anyway, hoping is in the path...");
    }
    return mavenInstallation;
  }


  private boolean executeSonar(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener,
                               SonarInstallation sonarInstallation, Maven.MavenInstallation mavenInstallation) {

    String executable = buildExecName(launcher, mavenInstallation);
    String[] command = buildCommand(build, sonarInstallation, executable);
    Map<String, String> environmentVars = addM2HomeToEnvironmentVars(build, mavenInstallation);

    try {
      int r = launcher.launch(command, environmentVars, listener.getLogger(), build.getProject().getModuleRoot()).join();
      return r == 0;
    }
    catch (IOException e) {
      Util.displayIOException(e, listener);
      e.printStackTrace(listener.fatalError("command execution failed"));
      return false;
    }
    catch (InterruptedException e) {
      e.printStackTrace();
      return false;
    }
  }

  private static Map<String, String> addM2HomeToEnvironmentVars(AbstractBuild<?, ?> build, Maven.MavenInstallation mavenInstallation) {
    Map<String, String> environmentVars = build.getEnvVars();
    if (mavenInstallation != null) {
      environmentVars.put("M2_HOME", mavenInstallation.getMavenHome());
    }
    return environmentVars;
  }

  private static String buildExecName(Launcher launcher, Maven.MavenInstallation mavenInstallation) {
    String execName = launcher.isUnix() ? "mvn" : "mvn.bat";
    String executable = execName;
    if (mavenInstallation != null) {
      String mavenHome = mavenInstallation.getMavenHome();
      executable = mavenHome + File.separatorChar + "bin" + File.separatorChar + execName;
    }
    return executable;
  }

  private String[] buildCommand(AbstractBuild<?, ?> build, SonarInstallation sonarInstallation, String executable) {
    ArgumentListBuilder args = new ArgumentListBuilder();
    args.add(executable);
    args.addKeyValuePairs("-D", build.getBuildVariables());
    args.addTokenized(sonarInstallation.getPluginCallArgs());
    args.addTokenized(getJobAdditionalProperties());
    return args.toCommandArray();
  }

  public Descriptor<Publisher> getDescriptor() {
    return DESCRIPTOR;
  }

  public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

  public static final class DescriptorImpl extends Descriptor<Publisher> {
    @CopyOnWrite
    private volatile SonarInstallation[] installations = new SonarInstallation[0];

    DescriptorImpl() {
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
      installations = req.bindParametersToList(
          SonarInstallation.class, "sonar.").toArray(new SonarInstallation[0]);
      save();
      return true;
    }

    @Override
    public Publisher newInstance(StaplerRequest req) {
      return req.bindParameters(SonarPublisher.class, "sonar.");
    }

    // web methods

    public void doCheckSonarVersion(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
      final String version = req.getParameter("value");
      // this can be used to check the existence of a file on the server, so needs to be protected
      new FormFieldValidator(req, rsp, true) {
        @Override
        public void check() throws IOException, ServletException {
          if (version == null || !version.contains(".")) {
            error("Sonar version is not valid.");
            return;
          }

          ok();
        }
      }.process();
    }
  }
}
