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
import hudson.model.Run;
import hudson.plugins.sonar.utils.SonarUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import jenkins.model.TransientActionFactory;

@Extension
public class SonarBuildBadgeActionFactory extends TransientActionFactory<Run> {

  @Override
  public Class<Run> type() {
    return Run.class;
  }

  @Override
  /**
   * Creates a {@link BuildSonarAction} if a sonar analysis was performed in the run.
   * The badge will have an URL if there aren't multiple URLs.
   */
  public Collection<? extends Action> createFor(Run run) {
    List<SonarAnalysisAction> actions = SonarUtils.getPersistentActions(run, SonarAnalysisAction.class);

    if (actions.isEmpty()) {
      return Collections.emptyList();
    }

    String url = null;

    for (SonarAnalysisAction a : actions) {
      // with workflows, we don't have realtime access to build logs, so url might be null
      // it might also have failed, but we still want to show the wave
      if (a.getUrl() != null) {
        if (url == null) {
          url = a.getUrl();
        } else if (!url.equals(a.getUrl())) {
          // there are several different URLs, so we don't display any URL
          url = null;
          break;
        }
      }
    }

    return Collections.singletonList(new SonarBuildBadgeAction(url));
  }
}
