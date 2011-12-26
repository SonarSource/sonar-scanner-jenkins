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

/**
 * @since 1.2
 * @deprecated in 1.7
 */
public class LightProjectConfig {
  /**
   * Mandatory and no spaces.
   */
  private String groupId;

  /**
   * Mandatory and no spaces.
   */
  private String artifactId;

  /**
   * Mandatory.
   */
  private String projectName;

  /**
   * Optional.
   */
  private String projectVersion;

  /**
   * Optional.
   */
  private String projectDescription;

  /**
   * Optional.
   */
  private String javaVersion;

  /**
   * Mandatory.
   */
  private String projectSrcDir;

  /**
   * Optional.
   */
  private String projectSrcEncoding;

  /**
   * Optional.
   */
  private String projectBinDir;

  /**
   * Optional.
   */
  private ReportsConfig reports;

  private LightProjectConfig() {
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

  public ReportsConfig getReports() {
    return reports;
  }

  public boolean isReuseReports() {
    return reports != null;
  }
}
