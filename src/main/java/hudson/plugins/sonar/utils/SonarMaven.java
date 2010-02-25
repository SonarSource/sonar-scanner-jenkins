package hudson.plugins.sonar.utils;

import hudson.Launcher;
import hudson.maven.MavenModuleSet;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.plugins.sonar.SonarInstallation;
import hudson.plugins.sonar.SonarPublisher;
import hudson.tasks.Maven;
import hudson.util.ArgumentListBuilder;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;

/**
 * @author Evgeny Mandrikov
 * @since 1.3
 */
public final class SonarMaven extends Maven {
  /**
   * Produce execution error messages and run in non-interactive (batch) mode.
   */
  private static final String TARGET = "-e -B sonar:sonar";

  private SonarInstallation sonarInstallation;

  public SonarMaven(String targets, String name, String pom, String jvmOptions, boolean usePrivateRepository, SonarInstallation sonarInstallation) {
    super(targets + " " + TARGET, name, pom, "", jvmOptions, usePrivateRepository);
    this.sonarInstallation = sonarInstallation;
  }

  @Override
  protected void wrapUpArguments(ArgumentListBuilder args, String normalizedTarget, AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
      throws IOException, InterruptedException {
    ExtendedArgumentListBuilder argsBuilder = new ExtendedArgumentListBuilder(args, launcher.isUnix());
    argsBuilder.append("sonar.jdbc.driver", sonarInstallation.getDatabaseDriver());
    argsBuilder.append("sonar.jdbc.url", sonarInstallation.getDatabaseUrl());  // TODO can be masked
    argsBuilder.appendMasked("sonar.jdbc.username", sonarInstallation.getDatabaseLogin());
    argsBuilder.appendMasked("sonar.jdbc.password", sonarInstallation.getDatabasePassword());
    argsBuilder.append("sonar.host.url", sonarInstallation.getServerUrl());
  }

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) Hudson.getInstance().getDescriptorOrDie(Maven.class);
  }

  public static boolean executeMaven(
      AbstractBuild<?, ?> build,
      Launcher launcher,
      BuildListener listener,
      String mavenName,
      String pom,
      SonarInstallation sonarInstallation,
      SonarPublisher sonarPublisher
  ) throws IOException, InterruptedException {
    MavenModuleSet mavenModuleProject = sonarPublisher.getMavenProject(build);
    /**
     * MAVEN_OPTS
     */
    String jvmOptions = sonarPublisher.getMavenOpts();
    if (StringUtils.isEmpty(jvmOptions)) {
      if (mavenModuleProject != null && StringUtils.isNotEmpty(mavenModuleProject.getMavenOpts())) {
        jvmOptions = mavenModuleProject.getMavenOpts();
      }
    }
    // Private Repository and Alternate Settings
    boolean usesPrivateRepository = false;
    String alternateSettings = null;
    if (mavenModuleProject != null) {
      usesPrivateRepository = mavenModuleProject.usesPrivateRepository();
      alternateSettings = mavenModuleProject.getAlternateSettings();
    }
    // Other properties
    String installationProperties = sonarInstallation.getAdditionalProperties();
    String jobProperties = sonarPublisher.getJobAdditionalProperties();
    String aditionalProperties = ""
        + (StringUtils.isNotBlank(installationProperties) ? installationProperties : "") + " "
        + (StringUtils.isNotBlank(jobProperties) ? jobProperties : "") + " "
        + (StringUtils.isNotBlank(alternateSettings) ? "-s " + alternateSettings : "");
    // Execute Maven
    return new SonarMaven(aditionalProperties, mavenName, pom, jvmOptions, usesPrivateRepository, sonarInstallation)
        .perform(build, launcher, listener);
  }
}
