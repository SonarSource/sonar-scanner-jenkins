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
/*
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package hudson.plugins.sonar;

import hudson.PluginWrapper;
import hudson.model.BuildBadgeAction;
import hudson.plugins.sonar.utils.MagicNames;
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
public final class BuildSonarAction implements BuildBadgeAction {

  public final String url;

  public BuildSonarAction() {
    this.url = null;
  }

  public BuildSonarAction(String url) {
    this.url = url;
  }

  public String getTooltip() {
    return Messages.BuildSonarAction_Tooltip();
  }

  public String getDisplayName() {
    return Messages.SonarAction_Sonar();
  }

  public String getIcon() {
    PluginWrapper wrapper = Jenkins.getInstance().getPluginManager()
        .getPlugin(SonarPlugin.class);
    return "/plugin/" + wrapper.getShortName() + "/images/" + MagicNames.ICON;
  }

  // non use interface methods
  public String getIconFileName() {
    return null;
  }

  public String getUrlName() {
    return url;
  }

  @Exported(visibility = 2)
  public String getUrl() {
    return url;
  }
}
