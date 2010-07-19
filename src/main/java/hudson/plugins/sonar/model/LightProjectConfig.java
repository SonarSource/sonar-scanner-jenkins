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
package hudson.plugins.sonar.model;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Evgeny Mandrikov
 * @since 1.2
 */
public class LightProjectConfig {
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

  /**
   * Optional.
   */
  private final String language;

  /**
   * Optional.
   */
  private final ReportsConfig reports;

  public LightProjectConfig(String groupId, String artifactId, String projectName) {
    this(groupId, artifactId, projectName, null, null, null, null, null, null, null, null);
  }

  @DataBoundConstructor
  public LightProjectConfig(
      String groupId,
      String artifactId,
      String projectName,
      String projectVersion,
      String projectDescription,
      String javaVersion,
      String projectSrcDir,
      String projectSrcEncoding,
      String projectBinDir,
      String language,
      ReportsConfig reports
  ) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.projectName = projectName;
    this.projectVersion = projectVersion;
    this.projectDescription = projectDescription;
    this.javaVersion = javaVersion;
    this.projectSrcDir = projectSrcDir;
    this.projectSrcEncoding = projectSrcEncoding;
    this.projectBinDir = projectBinDir;
    this.language = language;
    this.reports = reports;
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

  public String getProjectDescription() {
    return StringUtils.trimToEmpty(projectDescription);
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

  public String getLanguage() {
    return StringUtils.trimToEmpty(language);
  }

  public ReportsConfig getReports() {
    return reports;
  }

  public boolean isReuseReports() {
    return reports != null;
  }
}
