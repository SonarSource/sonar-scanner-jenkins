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
  private static final String TARGET = "-e -B";

  private SonarPublisher publisher;

  public SonarMaven(String targets, String name, String pom, String jvmOptions, boolean usePrivateRepository, SonarPublisher publisher) {
    super(targets + " " + getTarget(publisher.getInstallation()), name, pom, "", jvmOptions, usePrivateRepository);
    this.publisher = publisher;
  }

  private static String getTarget(SonarInstallation installation) {
    if (StringUtils.isBlank(installation.getMojoVersion())) {
      return TARGET + " sonar:sonar";
    } else {
      return TARGET + " org.codehaus.mojo:sonar-maven-plugin:1.0-beta-1:sonar";
    }
  }

  private SonarInstallation getInstallation() {
    return publisher.getInstallation();
  }

  @Override
  protected void wrapUpArguments(ArgumentListBuilder args, String normalizedTarget, AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
      throws IOException, InterruptedException {
    ExtendedArgumentListBuilder argsBuilder = new ExtendedArgumentListBuilder(args, launcher.isUnix());
    argsBuilder.append("sonar.jdbc.driver", getInstallation().getDatabaseDriver());
    argsBuilder.append("sonar.jdbc.url", getInstallation().getDatabaseUrl());  // TODO can be masked
    argsBuilder.appendMasked("sonar.jdbc.username", getInstallation().getDatabaseLogin());
    argsBuilder.appendMasked("sonar.jdbc.password", getInstallation().getDatabasePassword());
    argsBuilder.append("sonar.host.url", getInstallation().getServerUrl());

    argsBuilder.append("sonar.branch", publisher.getBranch());
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
    pom = build.getModuleRoot().child(pom).getRemote(); // SONARPLUGINS-487
    return new SonarMaven(aditionalProperties, mavenName, pom, jvmOptions, usesPrivateRepository, sonarPublisher)
        .perform(build, launcher, listener);
  }
}
