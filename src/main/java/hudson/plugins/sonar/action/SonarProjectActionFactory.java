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

import hudson.Extension;
import hudson.model.Action;
import hudson.model.Actionable;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.model.Job;
import hudson.model.ProminentProjectAction;
import hudson.model.Run;
import hudson.plugins.sonar.SonarBuildWrapper;
import hudson.plugins.sonar.client.HttpClient;
import hudson.plugins.sonar.client.ProjectInformation;
import hudson.plugins.sonar.client.SQProjectResolver;
import hudson.plugins.sonar.utils.SonarUtils;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import jenkins.model.TransientActionFactory;

@Extension
/**
 * We don't use {@link TransientProjectActionFactory} because it appears to be cached and requires Jenkins to restart.
 */
public class SonarProjectActionFactory extends TransientActionFactory<Job> {
  private SQProjectResolver resolver;

  public SonarProjectActionFactory() {
    resolver = new SQProjectResolver(new HttpClient());
  }

  @Override
  public Class<Job> type() {
    return Job.class;
  }

  @Override
  public Collection<? extends Action> createFor(Job project) {
    Set<String> urls = new HashSet<>();
    List<ProminentProjectAction> sonarProjectActions = new LinkedList<>();
    List<SonarAnalysisAction> filteredActions = new LinkedList<>();

    // don't fetch builds that haven't finished yet
    Run<?, ?> lastBuild = project.getLastCompletedBuild();

    if (lastBuild != null) {
      for (SonarAnalysisAction a : lastBuild.getActions(SonarAnalysisAction.class)) {
        if (a.getUrl() != null && !urls.contains(a.getUrl())) {
          urls.add(a.getUrl());
          sonarProjectActions.add(new SonarProjectIconAction(a));
          filteredActions.add(a);
        }
      }
    }

    if (sonarProjectActions.isEmpty()) {
      if (projectHasSonarAnalysis(project)) {
        // display at least 1 wave without any URL in the project page
        sonarProjectActions.add(new SonarProjectIconAction());
      }
    } else {
      SonarProjectPageAction projectPage = createProjectPage(lastBuild, filteredActions);
      if (projectPage != null) {
        sonarProjectActions.add(projectPage);
      }
    }

    return sonarProjectActions;
  }

  /**
   * Returns whether the project has any Sonar analysis currently configured.
   * The goal is to not display anything if no analysis is currently configured, even if the latest build did perform an analysis
   */
  private static boolean projectHasSonarAnalysis(Job project) {
    if (project instanceof BuildableItemWithBuildWrappers) {
      // SonarBuildWrapper is no more able to contribute project actions since it was made compatible with pipeline
      for (Object wrapper : ((BuildableItemWithBuildWrappers) project).getBuildWrappersList()) {
        if (wrapper instanceof SonarBuildWrapper) {
          return true;
        }
      }
    }
    return !SonarUtils.getPersistentActions(project, SonarMarkerAction.class).isEmpty();
  }

  /**
   * Action that will create the jelly section in the Project page
   */
  @CheckForNull
  private SonarProjectPageAction createProjectPage(Run<?, ?> run, List<SonarAnalysisAction> actions) {
    long endTime = run.getStartTimeInMillis() + run.getDuration();
    List<ProjectInformation> projects;

    synchronized (run) {
      SonarCacheAction cache = getOrCreateCache(run);
      projects = cache.get(resolver, endTime, actions);
    }

    if (projects == null || projects.isEmpty()) {
      return null;
    }
    return new SonarProjectPageAction(projects);
  }

  private static SonarCacheAction getOrCreateCache(Actionable actionable) {
    SonarCacheAction cache = SonarUtils.getPersistentAction(actionable, SonarCacheAction.class);
    if (cache == null) {
      cache = new SonarCacheAction();
      actionable.addAction(cache);
    }
    return cache;
  }
}
