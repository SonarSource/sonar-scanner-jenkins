package hudson.plugins.sonar;

import hudson.Launcher;
import hudson.maven.MavenModuleSet;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.Maven;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;

/**
 * @author Evgeny Mandrikov
 */
public class MavenHelper {
  /**
   * Produce execution error messages and run in non-interactive (batch) mode.
   */
  public static String MAVEN_PROPERTIES = "-e -B";

  /**
   * See {@link hudson.tasks.Maven}.
   *
   * @param build                build
   * @param launcher             launcher
   * @param listener             listener
   * @param targets              The targets and other maven options like "-Pprofile".
   *                             Can be separated by SP or NL.
   * @param mavenName            Identifies {@link hudson.tasks.Maven.MavenInstallation} to be used.
   * @param pom                  Optional POM file path relative to the workspace.
   *                             Used for the Maven '-f' option.
   * @param properties           Optional properties to be passed to Maven like "-Dname=value".
   *                             Follows {@link java.util.Properties} syntax.
   * @param jvmOptions           MAVEN_OPTS if not null.
   * @param usePrivateRepository If true, the build will use its own local Maven repository
   *                             via "-Dmaven.repo.local=...".
   * @return true if the build can continue, false if there was an error
   *         and the build needs to be aborted.
   * @throws IOException          If the implementation wants to abort the processing when an {@link IOException}
   *                              happens
   * @throws InterruptedException If the build is interrupted by the user.
   */
  public static boolean executeMaven(
      AbstractBuild<?, ?> build,
      Launcher launcher,
      BuildListener listener,
      String targets,
      String mavenName,
      String pom,
      String properties,
      String jvmOptions,
      boolean usePrivateRepository
  ) throws IOException, InterruptedException {
    return new Maven(targets, mavenName, pom, properties, jvmOptions, usePrivateRepository)
        .perform(build, launcher, listener);
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
     * Replacement for {@link SonarPublisher#getMavenEnvironmentVars}
     */
    String jvmOptions = sonarPublisher.getMavenOpts();
    if (StringUtils.isEmpty(jvmOptions)) {
      if (mavenModuleProject != null && StringUtils.isNotEmpty(mavenModuleProject.getMavenOpts())) {
        jvmOptions = mavenModuleProject.getMavenOpts();
      }
    }
    /**
     * Replacement for {@link SonarPublisher#buildCommand}
     */
    // Private Repository
    boolean usesPrivateRepository = false;
    if (mavenModuleProject != null) {
      usesPrivateRepository = mavenModuleProject.usesPrivateRepository();
    }
    // -D properties
    String pluginCallArgs = sonarInstallation.getPluginCallArgs();
    // Other properties
    String installationProperties = sonarInstallation.getAdditionalProperties();
    String jobProperties = sonarPublisher.getJobAdditionalProperties();
    String aditionalProperties = ""
        + (StringUtils.isNotBlank(installationProperties) ? installationProperties : "") + " "
        + (StringUtils.isNotBlank(jobProperties) ? jobProperties : "");
    // Execute Maven
    return MavenHelper.executeMaven(
        build,
        launcher,
        listener,
        aditionalProperties + " " + MAVEN_PROPERTIES + " sonar:sonar",
        mavenName,
        pom,
        pluginCallArgs,
        jvmOptions,
        usesPrivateRepository
    );
  }
}
