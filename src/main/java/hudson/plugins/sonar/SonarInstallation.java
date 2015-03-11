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
package hudson.plugins.sonar;

import hudson.Util;
import hudson.plugins.sonar.model.TriggersConfig;
import hudson.util.Scrambler;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

public class SonarInstallation {

  /**
   * @return all available installations, never <tt>null</tt>
   * @since 1.7
   */
  public static final SonarInstallation[] all() {
    Jenkins jenkins = Jenkins.getInstance();
    if (jenkins == null) {
      // for unit test
      return new SonarInstallation[0];
    }
    SonarPublisher.DescriptorImpl sonarDescriptor = Jenkins.getInstance().getDescriptorByType(SonarPublisher.DescriptorImpl.class);
    return sonarDescriptor.getInstallations();
  }

  /**
   * @return installation by name, <tt>null</tt> if not found
   * @since 1.7
   */
  public static final SonarInstallation get(String name) {
    SonarInstallation[] available = all();
    if (StringUtils.isEmpty(name) && available.length > 0) {
      return available[0];
    }
    for (SonarInstallation si : available) {
      if (StringUtils.equals(name, si.getName())) {
        return si;
      }
    }
    return null;
  }

  private final String name;
  private final boolean disabled;
  private final String serverUrl;

  /**
   * @since 1.5
   */
  private String mojoVersion;

  private final String databaseUrl;
  private final String databaseLogin;

  /**
   * @deprecated since 2.2
   */
  @Deprecated
  private transient String databasePassword;
  private final String additionalProperties;

  private TriggersConfig triggers;

  /**
   * @since 2.0.1
   */
  private String sonarLogin;

  /**
   * @deprecated since 2.2
   */
  @Deprecated
  private transient String sonarPassword;

  /**
   * @since 2.2
   */
  private Secret databaseSecret;
  private Secret sonarSecret;

  @DataBoundConstructor
  public SonarInstallation(String name, boolean disabled,
    String serverUrl,
    String databaseUrl, String databaseLogin, String databasePassword,
    String mojoVersion, String additionalProperties, TriggersConfig triggers,
    String sonarLogin, String sonarPassword) {
    this.name = name;
    this.disabled = disabled;
    this.serverUrl = serverUrl;
    this.databaseUrl = databaseUrl;
    this.databaseLogin = databaseLogin;
    setDatabasePassword(databasePassword);
    this.mojoVersion = mojoVersion;
    this.additionalProperties = additionalProperties;
    this.triggers = triggers;
    this.sonarLogin = sonarLogin;
    setSonarPassword(sonarPassword);
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
   * @return version of sonar-maven-plugin to use
   * @since 1.5
   */
  public String getMojoVersion() {
    return mojoVersion;
  }

  public String getDatabaseUrl() {
    return databaseUrl;
  }

  public String getDatabaseLogin() {
    return databaseLogin;
  }

  public String getDatabasePassword() {
    return Secret.toString(databaseSecret);
  }

  /**
   * @since 1.7
   */
  public final void setDatabasePassword(String password) {
    databaseSecret = Secret.fromString(Util.fixEmptyAndTrim(password));
  }

  /**
   * For internal use only. Allows to perform migration.
   *
   * @since 1.7
   */
  public String getScrambledDatabasePassword() {
    return databaseSecret.getEncryptedValue();
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

  /**
   * @since 2.0.1
   */
  public String getSonarLogin() {
    return sonarLogin;
  }

  /**
   * @since 2.0.1
   */
  public String getSonarPassword() {
    return Secret.toString(sonarSecret);
  }

  public final void setSonarPassword(String sonarPassword) {
    sonarSecret = Secret.fromString(Util.fixEmptyAndTrim(sonarPassword));
  }

  private Object readResolve() {
    // Perform password migration to Secret (SONARJNKNS-201)
    // Data will be persisted when SonarPublisher.DescriptorImpl is saved.

    if (databasePassword != null) {
      setDatabasePassword(Scrambler.descramble(databasePassword));
      databasePassword = null;
    }

    if (sonarPassword != null) {
      setSonarPassword(Scrambler.descramble(sonarPassword));
      sonarPassword = null;
    }

    return this;
  }
}
