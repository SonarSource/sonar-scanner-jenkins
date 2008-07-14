package hudson.plugins.sonar;

import hudson.CopyOnWrite;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.Publisher;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormFieldValidator;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;

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


  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
    SonarInstallation sonarInstallation = getInstallation();
    if (sonarInstallation == null) {
      listener.getLogger().println("No Sonar installation on this job. " + installationName + "," + DESCRIPTOR.getInstallations().length);
      return true;
    }
    if (sonarInstallation.isDisabled()) {
      listener.getLogger().println("Sonar is disabled (version " + sonarInstallation.getName() + "). See Hudson global configuration.");
      return true;

    }
    return executeSonar(build, launcher, listener, sonarInstallation);
  }

  private boolean executeSonar(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, SonarInstallation sonarInstallation) {
    ArgumentListBuilder args = new ArgumentListBuilder();
    String execName;
    if (launcher.isUnix()) {
      execName = "mvn";
    } else {
      execName = "mvn.bat";
    }

    args.add(execName);
    args.addKeyValuePairs("-D", build.getBuildVariables());
    args.addTokenized("org.codehaus.sonar:sonar-maven-plugin:" + sonarInstallation.getVersion() + ":sonar");

    if (!"".equals(StringUtils.defaultString(sonarInstallation.getDatabaseDriver()))) {
      args.add("-Dsonar.jdbc.driver=" + sonarInstallation.getDatabaseDriver());
    }
    if (!"".equals(StringUtils.defaultString(sonarInstallation.getDatabaseLogin()))) {
      args.add("-Dsonar.jdbc.username=" + sonarInstallation.getDatabaseLogin());
    }
    if (!"".equals(StringUtils.defaultString(sonarInstallation.getDatabasePassword()))) {
      args.add("-Dsonar.jdbc.password=" + sonarInstallation.getDatabasePassword());
    }
    if (!"".equals(StringUtils.defaultString(sonarInstallation.getDatabaseUrl()))) {
      args.add("-Dsonar.jdbc.url=" + sonarInstallation.getDatabaseUrl());
    }
    if (!"".equals(StringUtils.defaultString(sonarInstallation.getServerUrl()))) {
      args.add("-Dsonar.host.url=" + sonarInstallation.getServerUrl());
    }
    args.add("-Dsonar.skipInstall=true");

    String command = args.toStringWithQuote();
    command += " " + getJobAdditionalProperties();
    command += " " + sonarInstallation.getAdditionalProperties();

    try {
      int r = launcher.launch(command, build.getEnvVars(), listener.getLogger(), build.getProject().getModuleRoot()).join();
      return r == 0;

    } catch (IOException e) {
      Util.displayIOException(e, listener);
      e.printStackTrace(listener.fatalError("command execution failed"));
      return false;

    } catch (InterruptedException e) {
      e.printStackTrace();
      return false;
    }
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

    public String getDisplayName() {
      return "Sonar";
    }

    public String getHelpFile() {
      return "/plugin/sonar/help.html";
    }

    public SonarInstallation[] getInstallations() {
      return installations;
    }

    public boolean configure(StaplerRequest req) {
      installations = req.bindParametersToList(
          SonarInstallation.class, "sonar.").toArray(new SonarInstallation[0]);
      save();
      return true;
    }

    public Publisher newInstance(StaplerRequest req) {
      return req.bindParameters(SonarPublisher.class, "sonar.");
    }

    // web methods

    public void doCheckSonarVersion(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
      final String version = req.getParameter("value");
      // this can be used to check the existence of a file on the server, so needs to be protected
      new FormFieldValidator(req, rsp, true) {
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
