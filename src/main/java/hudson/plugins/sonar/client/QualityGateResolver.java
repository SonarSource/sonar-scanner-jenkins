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
package hudson.plugins.sonar.client;

import hudson.plugins.sonar.SonarInstallation;
import hudson.plugins.sonar.client.WsClient.ProjectQualityGate;
import hudson.plugins.sonar.utils.Logger;
import hudson.plugins.sonar.utils.SonarUtils;
import org.apache.commons.lang.StringUtils;

import javax.annotation.CheckForNull;

import java.util.logging.Level;

public class QualityGateResolver {
  private final HttpClient client;

  public QualityGateResolver(HttpClient client) {
    this.client = client;
  }
  
  /**
   * Resolve information concerning the quality gate.
   * Might return null if it's not possible to fetch it, which should be interpreted as 'nothing to display'.
   * Errors that should be displayed are included in {@link ProjectInformation#getErrors()}.
   */
  @CheckForNull
  public ProjectInformation get(String projectUrl, String installationName) {
    SonarInstallation inst = SonarInstallation.get(installationName);
    if (inst == null) {
      Logger.LOG.info("Invalid installation name: " + installationName);
      return null;
    }

    try {
      String projectKey = extractProjectKey(projectUrl);
      String serverUrl = extractServerUrl(projectUrl);
      WsClient wsClient = new WsClient(client, serverUrl, projectKey, inst.getSonarLogin(), inst.getSonarPassword());
      
      Float version = SonarUtils.extractMajorMinor(wsClient.getServerVersion(serverUrl));
      if (version == null || !checkServerUrl(serverUrl, inst)) {
        return null;
      }

      ProjectQualityGate qg;
      if (version < 5.2f) {
        qg = wsClient.getQualityGateBefore52();
      } else {
        qg = wsClient.getQualityGate52();
      }

      ProjectInformation projectInfo = new ProjectInformation(projectKey);
      projectInfo.setName(qg.getProjectName());
      projectInfo.setStatus(qg.getStatus());
      projectInfo.setUrl(projectUrl);
      return projectInfo;

    } catch (Exception e) {
      Logger.LOG.log(Level.WARNING, "Error fetching project information", e);
      return null;
    }
  }

  private static boolean checkServerUrl(String serverUrl, SonarInstallation inst) {
    String configUrl = StringUtils.isEmpty(inst.getServerUrl()) ? "http://localhost:9000" : inst.getServerUrl();

    if (!configUrl.equals(serverUrl)) {
      Logger.LOG.info(String.format("Inconsistent server URL: '%s' parsed, '%s' configured", serverUrl, configUrl));
      return false;
    }

    return true;
  }

  static String extractServerUrl(String url) {
    return StringUtils.substringBefore(url, "/dashboard");
  }

  static String extractProjectKey(String url) {
    return StringUtils.substringAfterLast(url, "/dashboard/index/");
  }
}
