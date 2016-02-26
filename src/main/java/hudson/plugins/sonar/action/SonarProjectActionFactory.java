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
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.ProminentProjectAction;
import hudson.model.Run;
import hudson.plugins.sonar.client.HttpClient;
import hudson.plugins.sonar.client.SQProjectResolver;
import hudson.plugins.sonar.utils.SonarUtils;
import jenkins.model.TransientActionFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Extension
/**
 * We don't use {@link TransientProjectActionFactory} because it appears to be cached and requires Jenkins to restart.
 */
public class SonarProjectActionFactory extends TransientActionFactory<AbstractProject> {
  @Override
  public Class<AbstractProject> type() {
    return AbstractProject.class;
  }

  @Override
  public Collection<? extends Action> createFor(AbstractProject project) {
    if (project == null || !projectHasSonarAnalysis(project)) {
      return Collections.emptyList();
    }

    List<ProminentProjectAction> sonarProjectActions = new LinkedList<ProminentProjectAction>();
    List<SonarAnalysisAction> filteredActions = new LinkedList<SonarAnalysisAction>();

    // don't fetch builds that haven't finished yet
    Run<?, ?> lastBuild = project.getLastCompletedBuild();

    if (lastBuild != null) {
      for (SonarAnalysisAction a : lastBuild.getActions(SonarAnalysisAction.class)) {
        if (a.getUrl() != null) {
          sonarProjectActions.add(new SonarProjectIconAction(a));
          filteredActions.add(a);
        }
      }
    }

    if (sonarProjectActions.isEmpty()) {
      // display at least 1 wave without any url on the project page
      sonarProjectActions.add(new SonarProjectIconAction());
    }

    if (!filteredActions.isEmpty()) {
      sonarProjectActions.add(createProjectPage(filteredActions));
    }

    return sonarProjectActions;
  }

  /**
   * Returns whether the project has any Sonar analysis currently configured.
   * The goal is to not display anything if no analysis is currently configured, even if the latest build did perform an analysis
   */
  private static boolean projectHasSonarAnalysis(AbstractProject project) {
    return !SonarUtils.getPersistentActions(project, SonarMarkerAction.class).isEmpty();
  }

  /**
   * Action that will create the jelly section in the Project page
   */
  private static SonarProjectPageAction createProjectPage(List<SonarAnalysisAction> actions) {
    return new SonarProjectPageAction(actions, new SQProjectResolver(new HttpClient()));
  }

}
