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

import hudson.AbortException;
import hudson.Util;
import hudson.model.TaskListener;
import hudson.plugins.sonar.model.TriggersConfig;
import hudson.util.Scrambler;
import hudson.util.Secret;
import java.io.Serializable;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import static hudson.plugins.sonar.utils.SQServerVersions.SQ_5_1_OR_LOWER;
import static hudson.plugins.sonar.utils.SQServerVersions.SQ_5_2;
import static hudson.plugins.sonar.utils.SQServerVersions.SQ_5_3_OR_HIGHER;

public class SonarInstallation implements Serializable {

  private static final long serialVersionUID = 1L;

  public static final String DEFAULT_SERVER_URL = "http://locahost:9000";

  private final String name;
  private final String serverUrl;

  /**
   * @since 2.4
   */
  private String serverVersion;

  /**
   * @since 2.4
   */
  private String serverAuthenticationToken;

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
  // command line arguments
  private final String additionalProperties;
  // key/value pairs
  private final String additionalAnalysisProperties;

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
  private String[] split;

  @DataBoundConstructor
  public SonarInstallation(String name,
    String serverUrl, String serverVersion, String serverAuthenticationToken,
    String databaseUrl, String databaseLogin, String databasePassword,
    String mojoVersion, String additionalProperties, TriggersConfig triggers,
    String sonarLogin, String sonarPassword, String additionalAnalysisProperties) {
    this.name = name;
    this.serverUrl = serverUrl;
    this.serverVersion = serverVersion;

    if (SQ_5_3_OR_HIGHER.equals(serverVersion)) {
      this.serverAuthenticationToken = serverAuthenticationToken;
      this.sonarLogin = "";
      setSonarPassword("");
    } else {
      this.serverAuthenticationToken = "";
      this.sonarLogin = sonarLogin;
      setSonarPassword(sonarPassword);
    }

    if (SQ_5_1_OR_LOWER.equals(serverVersion)) {
      this.databaseUrl = databaseUrl;
      this.databaseLogin = databaseLogin;
      setDatabasePassword(databasePassword);
    } else {
      this.databaseUrl = "";
      this.databaseLogin = "";
      setDatabasePassword("");
    }

    this.additionalAnalysisProperties = additionalAnalysisProperties;
    this.mojoVersion = mojoVersion;
    this.additionalProperties = additionalProperties;
    this.triggers = triggers;
  }

  /**
   * @return all available installations, never <tt>null</tt> but can be empty.
   * @since 1.7
   */
  public static final SonarInstallation[] all() {
    Jenkins jenkins = Jenkins.getInstance();
    if (jenkins == null) {
      // for unit test
      return new SonarInstallation[0];
    }
    SonarGlobalConfiguration sonarDescriptor = jenkins.getDescriptorByType(SonarGlobalConfiguration.class);
    return sonarDescriptor.getInstallations();
  }

  public static boolean isValid(String sonarInstallationName, TaskListener listener) {
    String failureMsg = validationMsg(sonarInstallationName);
    if (failureMsg != null) {
      listener.fatalError(failureMsg);
      return false;
    }
    return true;
  }

  public static void checkValid(String sonarInstallationName) throws AbortException {
    String failureMsg = validationMsg(sonarInstallationName);
    if (failureMsg != null) {
      throw new AbortException(failureMsg);
    }
  }

  private static String validationMsg(String sonarInstallationName) {
    String failureMsg;
    SonarInstallation sonarInstallation = SonarInstallation.get(sonarInstallationName);
    if (sonarInstallation == null) {
      if (StringUtils.isBlank(sonarInstallationName)) {
        failureMsg = Messages.SonarInstallation_NoInstallation(SonarInstallation.all().length);
      } else {
        failureMsg = Messages.SonarInstallation_NoMatchInstallation(sonarInstallationName, SonarInstallation.all().length);
      }
      failureMsg += "\n" + Messages.SonarInstallation_FixInstallationTip();
    } else {
      failureMsg = null;
    }
    return failureMsg;
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

  public String getName() {
    return name;
  }

  public String getServerUrl() {
    return serverUrl;
  }

  /**
   * @since 2.4
   */
  public String getServerAuthenticationToken() {
    return serverAuthenticationToken;
  }

  /**
   * serverVersion might be null when upgrading to 2.4.
   * Automatically figures out a value in that case.
   */
  public String getServerVersion() {
    return serverVersion;
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
  private final void setDatabasePassword(String password) {
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

  public String getAdditionalAnalysisProperties() {
    return additionalAnalysisProperties;
  }

  public String[] getAdditionalAnalysisPropertiesWindows() {
    if (additionalAnalysisProperties == null) {
      return new String[0];
    }

    split = StringUtils.split(additionalAnalysisProperties);
    for (int i = 0; i < split.length; i++) {
      split[i] = "/d:" + split[i];
    }
    return split;
  }

  public String[] getAdditionalAnalysisPropertiesUnix() {
    if (additionalAnalysisProperties == null) {
      return new String[0];
    }

    split = StringUtils.split(additionalAnalysisProperties);
    for (int i = 0; i < split.length; i++) {
      split[i] = "-D" + split[i];
    }
    return split;
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

  private final void setSonarPassword(String sonarPassword) {
    sonarSecret = Secret.fromString(Util.fixEmptyAndTrim(sonarPassword));
  }

  protected Object readResolve() {
    // Perform password migration to Secret (SONARJNKNS-201)
    // Data will be persisted when SonarGlobalConfiguration is saved.
    if (databasePassword != null) {
      setDatabasePassword(Scrambler.descramble(databasePassword));
      databasePassword = null;
    }

    if (sonarPassword != null) {
      setSonarPassword(Scrambler.descramble(sonarPassword));
      sonarPassword = null;
    }

    /*
     * serverVersion might be null when upgrading to 2.5.
     * Automatically migrate in that case
     */
    if (serverVersion == null) {
      if (!StringUtils.isEmpty(databaseUrl) || !StringUtils.isEmpty(databaseLogin)) {
        serverVersion = SQ_5_1_OR_LOWER;
      } else if (!StringUtils.isEmpty(getSonarPassword())) {
        serverVersion = SQ_5_2;
      } else {
        serverAuthenticationToken = sonarLogin;
        sonarLogin = null;
        serverVersion = SQ_5_3_OR_HIGHER;
      }
    }

    return this;
  }
}
