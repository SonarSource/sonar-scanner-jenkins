/*
 * SonarQube Scanner for Jenkins
 * Copyright (C) 2007-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package hudson.plugins.sonar;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.PasswordCredentials;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import hudson.AbortException;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.sonar.model.TriggersConfig;
import java.io.Serializable;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

public class SonarInstallation implements Serializable {

  private static final long serialVersionUID = 1L;

  public static final String DEFAULT_SERVER_URL = "http://localhost:9000";

  private final String name;
  private final String serverUrl;

  /**
   * @since 2.9
   */
  private String credentialsId;

  /**
   * @since 1.5
   */
  private String mojoVersion;

  // command line arguments
  private final String additionalProperties;
  // key/value pairs
  private final String additionalAnalysisProperties;

  private TriggersConfig triggers;

  private String[] split;

  @DataBoundConstructor
  public SonarInstallation(String name,
    String serverUrl, String credentialsId,
    String mojoVersion, String additionalProperties, TriggersConfig triggers,
    String additionalAnalysisProperties) {
    this.name = name;
    this.serverUrl = serverUrl;
    this.credentialsId = credentialsId;
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
    return SonarGlobalConfiguration.get().getInstallations();
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
   * @since 2.9
   */
  public String getServerAuthenticationToken(Run<?, ?> build) {
    if (credentialsId == null || build == null) {
      return null;
    }
    StandardCredentials cred = this.getCredentials(build);
    if (cred == null) {
      return null;
    }
    return ((PasswordCredentials) cred).getPassword().getPlainText();
  }

  public StandardCredentials getCredentials(Run<?, ?> build) {
    return CredentialsProvider.findCredentialById(credentialsId, StandardCredentials.class, build);
  }

  /**
   * @since 2.9
   */
  @SuppressWarnings("unused")
  public String getCredentialsId() {
    return credentialsId;
  }

  /**
   * @return version of sonar-maven-plugin to use
   * @since 1.5
   */
  @CheckForNull
  public String getMojoVersion() {
    return mojoVersion;
  }

  @CheckForNull
  public String getAdditionalProperties() {
    return additionalProperties;
  }

  @CheckForNull
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

}
