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
package hudson.plugins.sonar.action;

import com.google.common.annotations.VisibleForTesting;
import hudson.model.InvisibleAction;
import hudson.plugins.sonar.client.ProjectInformation;
import hudson.plugins.sonar.client.SQProjectResolver;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class SonarCacheAction extends InvisibleAction {
  private Map<String, ProjectInformation> infoByTaskId;
  private Map<String, ProjectInformation> infoByUrl;
  private Long lastRequest;
  private List<ProjectInformation> lastProjInfo;

  public SonarCacheAction() {
    this.infoByTaskId = new HashMap<>();
    this.infoByUrl = new HashMap<>();
  }

  public List<ProjectInformation> get(SQProjectResolver resolver, long lastBuildTime, List<SonarAnalysisAction> analysis) {
    if (lastRequest != null && age(lastRequest) < TimeUnit.SECONDS.toMillis(10)) {
      return lastProjInfo;
    }

    List<ProjectInformation> list = new ArrayList<>(analysis.size());

    for (SonarAnalysisAction a : analysis) {
      ProjectInformation proj = get(resolver, lastBuildTime, a);
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
  ProjectInformation get(SQProjectResolver resolver, long lastBuildTime, SonarAnalysisAction analysis) {
    String taskId = analysis.getCeTaskId();
    ProjectInformation cached;

    if (taskId != null) {
      cached = infoByTaskId.get(taskId);
    } else {
      cached = infoByUrl.get(analysis.getUrl());
    }

    if (isEntryValid(cached, lastBuildTime)) {
      return cached;
    }

    ProjectInformation proj = resolver.resolve(analysis.getUrl(), analysis.getCeTaskId(), analysis.getInstallationName());
    if (proj != null) {
      if (taskId != null) {
        infoByTaskId.put(analysis.getCeTaskId(), proj);
      } else {
        infoByUrl.put(analysis.getUrl(), proj);
      }
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
