/*
 * SonarQube Scanner for Jenkins
 * Copyright (C) 2007-2021 SonarSource SA
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
package hudson.plugins.sonar.action;

import com.google.common.annotations.VisibleForTesting;
import hudson.model.InvisibleAction;
import hudson.model.Run;
import hudson.plugins.sonar.client.ProjectInformation;
import hudson.plugins.sonar.client.SQProjectResolver;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class SonarCacheAction extends InvisibleAction {
  private Map<String, ProjectInformation> infoByTaskId;
  private Long lastRequest;
  private List<ProjectInformation> lastProjInfo;

  public SonarCacheAction() {
    this.infoByTaskId = new ConcurrentHashMap<>();
  }

  public List<ProjectInformation> get(SQProjectResolver resolver, long lastBuildTime, List<SonarAnalysisAction> analysis, Run<?, ?> run) {
    if (lastRequest != null && age(lastRequest) < TimeUnit.SECONDS.toMillis(30)) {
      return lastProjInfo;
    }

    List<ProjectInformation> list = new ArrayList<>(analysis.size());

    for (SonarAnalysisAction a : analysis) {
      ProjectInformation proj = get(resolver, lastBuildTime, a, run);
      if (proj != null) {
        list.add(proj);
      }
    }

    lastProjInfo = list;
    lastRequest = System.currentTimeMillis();
    return list;
  }

  @CheckForNull
  @VisibleForTesting
  ProjectInformation get(SQProjectResolver resolver, long lastBuildTime, SonarAnalysisAction analysis, Run<?, ?> run) {
    String taskId = analysis.getCeTaskId();

    if (taskId == null) {
      return null;
    }

    ProjectInformation cached = infoByTaskId.get(taskId);
    if (isEntryValid(cached, lastBuildTime)) {
      return cached;
    }

    ProjectInformation proj = resolver.resolve(analysis.getInstallationUrl(), analysis.getUrl(), taskId, analysis.getInstallationName(), run);
    if (proj != null) {
      infoByTaskId.put(taskId, proj);
    }

    return proj;
  }

  @VisibleForTesting
  static boolean isEntryValid(@Nullable ProjectInformation cached, long lastBuild) {
    if (cached == null) {
      return false;
    }

    String status = cached.getCeStatus();
    if (status != null) {
      // check if CE task is done -> info won't change
      if ("failed".equalsIgnoreCase(status) || "success".equalsIgnoreCase(status) || "canceled".equalsIgnoreCase(status)) {
        return true;
      }
    } else {
      // check if last build was before last update (giving 60sec margin)
      long age = diff(cached, lastBuild);
      if (age > TimeUnit.SECONDS.toMillis(60)) {
        return true;
      }
    }

    return false;
  }

  private static long age(long time) {
    return System.currentTimeMillis() - time;
  }

  private static long diff(ProjectInformation proj, long lastBuild) {
    return proj.created() - lastBuild;
  }
}
