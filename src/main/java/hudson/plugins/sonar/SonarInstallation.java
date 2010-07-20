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
package hudson.plugins.sonar;

import hudson.plugins.sonar.model.TriggersConfig;
import hudson.plugins.sonar.utils.MagicNames;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

public class SonarInstallation {
  private final String name;
  private final boolean disabled;
  private final String serverUrl;

  /**
   * @since 1.4
   */
  private String serverPublicUrl;

  /**
   * @since 1.5
   */
  private String mojoVersion;

  private final String databaseUrl;
  private final String databaseDriver;
  private final String databaseLogin;
  private final String databasePassword;
  private final String additionalProperties;

  private TriggersConfig triggers;

  public SonarInstallation(String name) {
    this(name, false, null, null, null, null, null, null, null, null, null);
  }

  @DataBoundConstructor
  public SonarInstallation(String name, boolean disabled,
                           String serverUrl, String serverPublicUrl,
                           String databaseUrl, String databaseDriver, String databaseLogin, String databasePassword,
                           String mojoVersion, String additionalProperties, TriggersConfig triggers) {
    this.name = name;
    this.disabled = disabled;
    this.serverUrl = serverUrl;
    this.serverPublicUrl = serverPublicUrl;
    this.databaseUrl = databaseUrl;
    this.databaseDriver = databaseDriver;
    this.databaseLogin = databaseLogin;
    this.databasePassword = databasePassword;
    this.mojoVersion = mojoVersion;
    this.additionalProperties = additionalProperties;
    this.triggers = triggers;
  }

  public String getName() {
    return name;
  }

  public boolean isDisabled() {
    return disabled;
  }

  public String getServerUrl() {
    return serverUrl;
  }

  /**
   * @return publically available URL for users of Sonar server
   * @since 1.4
   */
  public String getServerPublicUrl() {
    return serverPublicUrl;
  }

  /**
   * @param serverPublicUrl publically available URL for users of Sonar server
   * @since 1.4
   */
  public void setServerPublicUrl(String serverPublicUrl) {
    this.serverPublicUrl = serverPublicUrl;
  }

  /**
   * @return version of sonar-maven-plugin to use
   * @since 1.5
   */
  public String getMojoVersion() {
    return mojoVersion;
  }

  public String getDatabaseUrl() {
    return databaseUrl;
  }

  public String getDatabaseDriver() {
    return databaseDriver;
  }

  public String getDatabaseLogin() {
    return databaseLogin;
  }

  public String getDatabasePassword() {
    return databasePassword;
  }

  public String getAdditionalProperties() {
    return additionalProperties;
  }

  public TriggersConfig getTriggers() {
    if (triggers == null) {
      triggers = new TriggersConfig();
    }
    return triggers;
  }

  public String getServerLink() {
    String url = StringUtils.defaultIfEmpty(
        StringUtils.trimToEmpty(getServerPublicUrl()),
        StringUtils.trimToEmpty(getServerUrl())
    );
    url = StringUtils.defaultIfEmpty(url, MagicNames.DEFAULT_SONAR_URL);
    return StringUtils.chomp(url, "/");
  }

  private String getServerLink(String prefix, String groupId, String artifactId) {
    return getServerLink(prefix, groupId, artifactId, null);
  }

  private String getServerLink(String prefix, String groupId, String artifactId, String branch) {
    StringBuilder builder = new StringBuilder();
    builder.append(getServerLink())
        .append(prefix)
        .append(groupId).append(':').append(artifactId);
    if (StringUtils.isNotEmpty(branch)) {
      builder.append(':').append(branch);
    }
    return builder.toString();
  }

  /**
   * @param groupId    Group ID
   * @param artifactId Artifact ID
   * @param branch     branch
   * @return URL of Sonar project dashboard
   */
  public String getProjectLink(String groupId, String artifactId, String branch) {
    return getServerLink("/project/index/", groupId, artifactId, branch);
  }

  public String getComponentLink(String groupId, String artifactId) {
    return getServerLink("/components/index/", groupId, artifactId);
  }
}
