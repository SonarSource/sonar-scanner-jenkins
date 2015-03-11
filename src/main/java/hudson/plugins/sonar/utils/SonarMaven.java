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
package hudson.plugins.sonar.utils;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import hudson.maven.local_repo.DefaultLocalRepositoryLocator;
import hudson.maven.local_repo.PerJobLocalRepositoryLocator;
import hudson.maven.local_repo.LocalRepositoryLocator;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.Computer;
import hudson.model.JDK;
import hudson.plugins.sonar.SonarInstallation;
import hudson.plugins.sonar.SonarPublisher;
import hudson.tasks.Maven;
import hudson.util.ArgumentListBuilder;
import jenkins.model.Jenkins;
import jenkins.mvn.GlobalSettingsProvider;
import jenkins.mvn.SettingsProvider;
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

  private final SonarPublisher publisher;
  private final String additionalProperties;
  private JDK jdk;
  private final BuildListener listener;
  private final LocalRepositoryLocator locaRepository;

  public SonarMaven(String additionalProperties, String name, String pom, String jvmOptions, LocalRepositoryLocator locaRepository,
    SonarPublisher publisher, BuildListener listener, JDK jdk, SettingsProvider settings, GlobalSettingsProvider globalSettings) {
    super(getTarget(publisher.getInstallation()), name, pom, "", jvmOptions, false,
      settings, globalSettings);
    this.additionalProperties = additionalProperties;
    this.locaRepository = locaRepository;
    this.publisher = publisher;
    this.jdk = jdk;
    this.listener = listener;
  }

  /**
   * Visibility of a method has been relaxed for tests.
   */
  static String getTarget(SonarInstallation installation) {
    if (StringUtils.isBlank(installation.getMojoVersion())) {
      return TARGET + " sonar:sonar";
    } else {
      return TARGET + " org.codehaus.mojo:sonar-maven-plugin:" + installation.getMojoVersion() + ":sonar";
    }
  }

  private SonarInstallation getInstallation() {
    return publisher.getInstallation();
  }

  @Override
  protected void wrapUpArguments(ArgumentListBuilder args, String normalizedTarget, AbstractBuild<?, ?> build, Launcher launcher,
    BuildListener listener) throws IOException, InterruptedException {

    args.addTokenized(additionalProperties);

    ExtendedArgumentListBuilder argsBuilder = new ExtendedArgumentListBuilder(args, launcher.isUnix());
    argsBuilder.append("sonar.jdbc.url", getInstallation().getDatabaseUrl());
    argsBuilder.appendMasked("sonar.jdbc.username", getInstallation().getDatabaseLogin());
    argsBuilder.appendMasked("sonar.jdbc.password", getInstallation().getDatabasePassword());
    argsBuilder.append("sonar.host.url", getInstallation().getServerUrl());

    argsBuilder.append("sonar.branch", publisher.getBranch());

    if (StringUtils.isNotBlank(getInstallation().getSonarLogin())) {
      argsBuilder.appendMasked("sonar.login", getInstallation().getSonarLogin());
      argsBuilder.appendMasked("sonar.password", getInstallation().getSonarPassword());
    }

    if (build instanceof MavenModuleSetBuild) {
      FilePath localRepo = locaRepository.locate((MavenModuleSetBuild) build);
      if (localRepo != null) {
        args.add("-Dmaven.repo.local=" + localRepo.getRemote());
      }
    } else if (locaRepository instanceof PerJobLocalRepositoryLocator) {
      FilePath workspace = build.getWorkspace();
      if (workspace != null) {
        args.add("-Dmaven.repo.local=" + workspace.child(".repository"));
      }
    }
  }

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) Jenkins.getInstance().getDescriptorOrDie(Maven.class);
  }

  public static boolean executeMaven(
    AbstractBuild<?, ?> build,
    Launcher launcher,
    BuildListener listener,
    String mavenName,
    String pom,
    SonarInstallation sonarInstallation,
    SonarPublisher sonarPublisher,
    JDK jdk,
    SettingsProvider settings,
    GlobalSettingsProvider globalSettings,
    boolean usesLocalRepository) throws IOException, InterruptedException {
    MavenModuleSet mavenModuleProject = sonarPublisher.getMavenProject(build);
    EnvVars envVars = build.getEnvironment(listener);
    /**
     * MAVEN_OPTS
     */
    String mvnOptions = sonarPublisher.getMavenOpts();
    if (StringUtils.isEmpty(mvnOptions)
      && mavenModuleProject != null
      && StringUtils.isNotEmpty(mavenModuleProject.getMavenOpts())) {
      mvnOptions = mavenModuleProject.getMavenOpts();
    }
    // Private Repository and Alternate Settings
    LocalRepositoryLocator locaRepositoryToUse = usesLocalRepository ? new PerJobLocalRepositoryLocator() : new DefaultLocalRepositoryLocator();
    SettingsProvider settingsToUse = settings;
    GlobalSettingsProvider globalSettingsToUse = globalSettings;
    if (mavenModuleProject != null) {
      // If we are on a Maven job then take values from the job itself
      locaRepositoryToUse = mavenModuleProject.getLocalRepository();
      settingsToUse = mavenModuleProject.getSettings();
      globalSettingsToUse = mavenModuleProject.getGlobalSettings();
    }
    // Other properties
    String installationProperties = sonarInstallation.getAdditionalProperties();
    String jobProperties = envVars.expand(sonarPublisher.getJobAdditionalProperties());
    String aditionalProperties = ""
      + (StringUtils.isNotBlank(installationProperties) ? installationProperties : "") + " "
      + (StringUtils.isNotBlank(jobProperties) ? jobProperties : "");
    // Execute Maven
    // SONARPLUGINS-487
    pom = build.getModuleRoot().child(pom).getRemote();
    return new SonarMaven(aditionalProperties, mavenName, pom, mvnOptions, locaRepositoryToUse, sonarPublisher, listener, jdk, settingsToUse, globalSettingsToUse)
      .perform(build, launcher, listener);
  }

  @Override
  protected void buildEnvVars(EnvVars env, MavenInstallation mi) throws IOException, InterruptedException {
    super.buildEnvVars(env, mi);
    // Override JDK in case it is set on Sonar publisher
    if (jdk != null) {
      Computer computer = Computer.currentComputer();
      if (computer != null) {
        // just in case were not in a build
        jdk = jdk.forNode(computer.getNode(), listener);
      }
      jdk.buildEnvVars(env);
    }
  }
}
