/*
 * SonarQube Scanner for Jenkins
 * Copyright (C) 2007-2018 SonarSource SA
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
package hudson.plugins.sonar.client;

import hudson.plugins.sonar.SonarInstallation;
import hudson.plugins.sonar.client.WsClient.CETask;
import hudson.plugins.sonar.utils.Logger;
import hudson.plugins.sonar.utils.Version;
import java.net.URI;
import java.net.URISyntaxException;
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
  public ProjectInformation resolve(@Nullable String serverUrl, @Nullable String projectDashboardUrl, String ceTaskId, String installationName) {
    SonarInstallation inst = SonarInstallation.get(installationName);
    if (inst == null) {
      Logger.LOG.info(() -> "Invalid installation name: " + installationName);
      return null;
    }

    try {
      if (!checkServerUrl(serverUrl, inst)) {
        return null;
      }

      WsClient wsClient = WsClient.create(client, inst);
      Version version = new Version(wsClient.getServerVersion());

      if (version.compareTo(new Version("5.6")) < 0) {
        Logger.LOG.info(() -> "SQ < 5.6 is not supported");
        return null;
      }

      ProjectInformation projectInfo = new ProjectInformation();
      projectInfo.setUrl(projectDashboardUrl);
      String analysisId = requestCETaskDetails(wsClient, projectInfo, ceTaskId);

      if (analysisId != null) {
        projectInfo.setStatus(wsClient.requestQualityGateStatus(analysisId));
      }

      return projectInfo;

    } catch (Exception e) {
      Logger.LOG.log(Level.WARNING, "Error fetching project information", e);
      return null;
    }
  }

  @CheckForNull
  private static String requestCETaskDetails(WsClient wsClient, ProjectInformation projectInfo, String ceTaskId) {
    CETask ceTask = wsClient.getCETask(ceTaskId);
    projectInfo.setCeStatus(ceTask.getStatus());
    projectInfo.setCeUrl(ceTask.getUrl());
    projectInfo.setName(ceTask.getComponentName());
    return ceTask.getAnalysisId();
  }

  private static boolean checkServerUrl(@Nullable String serverUrl, SonarInstallation inst) {
    if (serverUrl == null) {
      Logger.LOG.info(() -> String.format(Locale.US, "Invalid server url. ServerUrl='%s'", serverUrl));
      return false;
    }
    String installationUrl = StringUtils.isEmpty(inst.getServerUrl()) ? SonarInstallation.DEFAULT_SERVER_URL : inst.getServerUrl();

    try {
      URI installationURI = new URI(installationUrl);
      URI serverURI = new URI(serverUrl);

      if (!installationURI.equals(serverURI)) {
        Logger.LOG.warning(() -> String.format(Locale.US, "Inconsistent server URL: '%s' parsed, '%s' configured", serverUrl, installationUrl));
        return false;
      }
    } catch (URISyntaxException e) {
      Logger.LOG.warning(() -> String.format(Locale.US, "Invalid URL: '%s' parsed, '%s' configured: %s", serverUrl, installationUrl, e.getMessage()));
      return false;
    }

    return true;
  }

}
