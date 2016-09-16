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
package hudson.plugins.sonar.utils;

import hudson.util.VersionNumber;
import jenkins.model.Jenkins;

/**
 * Utility class to encapsulate the details of routing information in the Jenkins web application.
 * Use this class to get Jenkins URLs and relative paths.
 */
public final class JenkinsRouter {

  public static final boolean BEFORE_V2 = Jenkins.getVersion().isOlderThan(new VersionNumber("2"));

  private JenkinsRouter() {
    throw new AssertionError("utility class, forbidden constructor");
  }

  public static String getGlobalToolConfigUrl() {
    return getRootUrl() + getGlobalToolConfigRelPath();
  }

  private static String getRootUrl() {
    return Jenkins.getInstance().getRootUrl();
  }

  private static String getGlobalToolConfigRelPath() {
    return BEFORE_V2 ? "configure" : "configureTools";
  }

}
