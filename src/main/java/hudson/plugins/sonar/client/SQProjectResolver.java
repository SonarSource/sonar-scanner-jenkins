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
import hudson.plugins.sonar.client.WsClient.CETask;
import hudson.plugins.sonar.client.WsClient.ProjectQualityGate;
import hudson.plugins.sonar.utils.Logger;
import hudson.plugins.sonar.utils.Version;
import java.util.Locale;
import java.util.logging.Level;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;

public class SQProjectResolver {
  private final HttpClient client;

  public SQProjectResolver(HttpClient client) {
    this.client = client;
  }

  /**
   * Resolve information concerning the quality gate.
   * Might return null if it's not possible to fetch it, which should be interpreted as 'nothing to display'.
   * Errors that should be displayed are included in {@link ProjectInformation#getErrors()}.
   */
  @CheckForNull
  public ProjectInformation resolve(@Nullable String projectUrl, @Nullable String ceTaskId, String installationName) {
    SonarInstallation inst = SonarInstallation.get(installationName);
    if (inst == null) {
      Logger.LOG.info(() -> "Invalid installation name: " + installationName);
      return null;
    }

    try {
      String projectKey = extractProjectKey(projectUrl);
      String serverUrl = extractServerUrl(projectUrl);

      if (!checkServerUrl(serverUrl, projectKey, inst)) {
        return null;
      }

      WsClient wsClient = WsClient.create(client, inst);
      Version version = new Version(wsClient.getServerVersion());

      ProjectInformation projectInfo = new ProjectInformation(projectKey);
      projectInfo.setUrl(projectUrl);

      getQualityGate(wsClient, projectInfo, projectKey, version);
      if (projectInfo.getStatus() == null) {
        // if QG is not available for the project, without errors, projectInfo will be empty
        return projectInfo;
      }

      getCETask(wsClient, projectInfo, ceTaskId);

      if (projectInfo.getProjectName() == null) {
        projectInfo.setName(wsClient.getProjectName(projectKey));
      }
      return projectInfo;

    } catch (Exception e) {
      Logger.LOG.log(Level.WARNING, "Error fetching project information", e);
      return null;
    }
  }

  private static void getCETask(WsClient wsClient, ProjectInformation projectInfo, @Nullable String ceTaskId) {
    if (ceTaskId == null) {
      return;
    }

    CETask ceTask = wsClient.getCETask(ceTaskId);
    projectInfo.setCeStatus(ceTask.getStatus());
    projectInfo.setCeUrl(ceTask.getUrl());

    if (ceTask.getComponentName() != null) {
      projectInfo.setName(ceTask.getComponentName());
    }
  }

  private static void getQualityGate(WsClient client, ProjectInformation proj, String projectKey, Version version) throws Exception {
    ProjectQualityGate qg;
    if (version.compareTo(new Version("5.4")) < 0) {
      qg = client.getQualityGateBefore54(projectKey);
    } else {
      qg = client.getQualityGate54(projectKey);
    }

    // happens in LTS if project is not assigned to a QG and there is no default QG
    if (qg == null) {
      return;
    }

    proj.setStatus(qg.getStatus());

    if (qg.getProjectName() != null) {
      proj.setName(qg.getProjectName());
    }
  }

  private static boolean checkServerUrl(@Nullable String serverUrl, @Nullable String projectKey, SonarInstallation inst) {
    if (serverUrl == null || projectKey == null) {
      Logger.LOG.info(() -> String.format(Locale.US, "Invalid project url. ServerUrl='%s', projectKey='%s'", serverUrl, projectKey));
      return false;
    }
    String configUrl = StringUtils.isEmpty(inst.getServerUrl()) ? SonarInstallation.DEFAULT_SERVER_URL : inst.getServerUrl();

    if (!configUrl.equals(serverUrl)) {
      Logger.LOG.warning(() -> String.format(Locale.US, "Inconsistent server URL: '%s' parsed, '%s' configured", serverUrl, configUrl));
      return false;
    }

    return true;
  }

  @CheckForNull
  static String extractServerUrl(@Nullable String url) {
    return StringUtils.substringBefore(url, "/dashboard");
  }

  @CheckForNull
  static String extractProjectKey(@Nullable String url) {
    return StringUtils.substringAfterLast(url, "/dashboard/index/");
  }
}
