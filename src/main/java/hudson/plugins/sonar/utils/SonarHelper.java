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

package hudson.plugins.sonar.utils;

import hudson.Launcher;
import hudson.Util;
import hudson.maven.MavenModuleSet;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.plugins.sonar.SonarInstallation;
import hudson.plugins.sonar.SonarPublisher;
import hudson.tasks.Maven;
import hudson.util.ArgumentListBuilder;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;

/**
 * @author Evgeny Mandrikov
 * @since 1.2
 */
public final class SonarHelper {
  /**
   * Produce execution error messages and run in non-interactive (batch) mode.
   */
  public static final String MAVEN_PROPERTIES = "-e -B";

  /**
   * Hide utility-class constructor.
   */
  private SonarHelper() {
  }

  public static void appendUnlessEmpty(StringBuilder builder, String key, String value) {
    if (StringUtils.isNotEmpty(StringUtils.defaultString(value))) {
      builder.append(" -D");
      builder.append(key);
      builder.append('=');
      builder.append(value.contains(" ") ? "'" + value + "'" : value);
    }
  }

  public static void addTokenizedAndQuoted(boolean isUnix, ArgumentListBuilder args, String argsString) {
    if (StringUtils.isNotBlank(argsString)) {
      for (String argToken : Util.tokenize(argsString)) {
        // see SONARPLUGINS-123 amperstand bug with windows..
        if (!isUnix && argToken.contains("&")) {
          args.addQuoted(argToken);
        } else {
          args.add(argToken);
        }
      }
    }
  }

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
   * @throws java.io.IOException  If the implementation wants to abort the processing when an
   *                              {@link java.io.IOException} happens
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
    // -D properties
    String pluginCallArgs = sonarInstallation.getPluginCallArgs();
    // Other properties
    String installationProperties = sonarInstallation.getAdditionalProperties();
    String jobProperties = sonarPublisher.getJobAdditionalProperties();
    String aditionalProperties = ""
        + (StringUtils.isNotBlank(installationProperties) ? installationProperties : "") + " "
        + (StringUtils.isNotBlank(jobProperties) ? jobProperties : "") + " "
        + (StringUtils.isNotBlank(alternateSettings) ? "-s " + alternateSettings : "");
    // Execute Maven
    return executeMaven(
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
