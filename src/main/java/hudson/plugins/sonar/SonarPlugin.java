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

import hudson.Plugin;

public class SonarPlugin extends Plugin {

  /**
   * @deprecated Used to track version changes in {@link SonarInstallation}. Moved to the right class.
   *
   * 0: Passwords stored in plain text
   * 1: Scrambled passwords
   * 2: Secret passwords. Field deprecated since this version.
   */
  /*package*/ @Deprecated Integer configVersion;

  @Override
  public void postInitialize() throws Exception {
    // IMPORTANT, otherwise configVersion would be always null
    load();

    if (configVersion == null) {
      configVersion = 0;
    }
  }
}
