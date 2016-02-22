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

import hudson.plugins.sonar.Messages;
import hudson.plugins.sonar.SonarPlugin;

import hudson.PluginWrapper;
import hudson.model.BuildBadgeAction;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * {@link BuildBadgeAction} that shows the build contains Sonar analysis.
 *
 * @author Evgeny Mandrikov
 * @since 1.2
 */
@ExportedBean
public final class SonarBuildBadgeAction implements BuildBadgeAction {

  private final String url;

  public SonarBuildBadgeAction() {
    this.url = null;
  }

  public SonarBuildBadgeAction(String url) {
    this.url = url;
  }

  public String getTooltip() {
    return Messages.BuildSonarAction_Tooltip();
  }

  @Override
  public String getDisplayName() {
    return Messages.SonarAction_Sonar();
  }

  public String getIcon() {
    PluginWrapper wrapper = Jenkins.getInstance().getPluginManager()
      .getPlugin(SonarPlugin.class);
    return "/plugin/" + wrapper.getShortName() + "/images/waves_16x16.png";
  }

  // non use interface methods
  @Override
  public String getIconFileName() {
    return null;
  }

  @Override
  public String getUrlName() {
    return url;
  }

  @Exported(visibility = 2)
  public String getUrl() {
    return url;
  }
}
