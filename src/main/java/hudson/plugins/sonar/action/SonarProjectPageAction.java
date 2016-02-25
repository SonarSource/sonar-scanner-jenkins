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

import hudson.model.InvisibleAction;
import hudson.model.ProminentProjectAction;
import hudson.plugins.sonar.client.ProjectInformation;

import java.util.List;

/**
 * Displays a jelly section in the Project page with information regarding Quality Gate
 * This is recreated every time something is loaded, so should be lightweight
 */
public class SonarProjectPageAction extends InvisibleAction implements ProminentProjectAction {
  private final List<ProjectInformation> projects;

  public SonarProjectPageAction(List<ProjectInformation> projects) {
    this.projects = projects;
  }

  /**
   * Called while building the jelly section 
   */
  public List<ProjectInformation> getProjects() {
    return projects;
  }
}
