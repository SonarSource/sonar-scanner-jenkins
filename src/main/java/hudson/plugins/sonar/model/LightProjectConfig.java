package hudson.plugins.sonar.model;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Evgeny Mandrikov
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

  public LightProjectConfig() {
  }

  public LightProjectConfig(String groupId, String artifactId, String projectName) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.projectName = projectName;
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
      ReportsConfig reports
  ) {
    this(groupId, artifactId, projectName);
    this.projectVersion = projectVersion;
    this.projectDescription = projectDescription;
    this.javaVersion = javaVersion;
    this.projectSrcDir = projectSrcDir;
    this.projectSrcEncoding = projectSrcEncoding;
    this.projectBinDir = projectBinDir;
    this.reports = reports;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public void setArtifactId(String artifactId) {
    this.artifactId = artifactId;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public void setProjectVersion(String projectVersion) {
    this.projectVersion = projectVersion;
  }

  public void setProjectDescription(String projectDescription) {
    this.projectDescription = projectDescription;
  }

  public void setJavaVersion(String javaVersion) {
    this.javaVersion = javaVersion;
  }

  public void setProjectSrcDir(String projectSrcDir) {
    this.projectSrcDir = projectSrcDir;
  }

  public void setProjectSrcEncoding(String projectSrcEncoding) {
    this.projectSrcEncoding = projectSrcEncoding;
  }

  public void setProjectBinDir(String projectBinDir) {
    this.projectBinDir = projectBinDir;
  }

  public void setReports(ReportsConfig reports) {
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

  public ReportsConfig getReports() {
    return reports;
  }

  public boolean isReuseReports() {
    return reports != null;
  }
}
