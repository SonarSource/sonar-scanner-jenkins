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
package hudson.plugins.sonar.client;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import hudson.model.Run;
import hudson.plugins.sonar.SonarInstallation;
import hudson.plugins.sonar.client.WsClient.CETask;
import hudson.plugins.sonar.utils.Logger;
import hudson.plugins.sonar.utils.Version;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonarqube.ws.client.HttpException;

public class SQProjectResolver {
  @VisibleForTesting
  static final Cache<String, Version> INSTANCE_VERSION_CACHE = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build();
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
  public ProjectInformation resolve(@Nullable String serverUrl, @Nullable String projectDashboardUrl, String ceTaskId, String installationName, Run<?, ?> build) {
    SonarInstallation inst = SonarInstallation.get(installationName);
    if (inst == null) {
      Logger.LOG.info(() -> "Invalid installation name: " + installationName);
      return null;
    }
    if (serverUrl == null) {
      Logger.LOG.info("No server url.");
      return null;
    }

    try {
      String serverAuthenticationToken = inst.getServerAuthenticationToken(build);
      WsClient wsClient = new WsClient(client, serverUrl, serverAuthenticationToken);

      Version version = INSTANCE_VERSION_CACHE.get(installationName, () -> new Version(wsClient.getServerVersion()));

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

    } catch (HttpException e) {
      if (e.code() == 404) {
        Logger.LOG.log(Level.FINE, "Error fetching project information: {0}", e.getMessage());
      } else {
        Logger.LOG.log(Level.WARNING, "Error fetching project information", e);
      }
      return null;
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
}
