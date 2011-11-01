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
import hudson.model.Hudson;

import java.io.IOException;

public class SonarPlugin extends Plugin {

  private Integer configVersion;

  @Override
  public void postInitialize() throws Exception {
    load(); // IMPORTANT, otherwise configVersion would be always null

    if (configVersion == null) {
      configVersion = 0;
    }
    if (configVersion < 1) {
      migrateToVersion1();
    }
  }

  /**
   * Scramble passwords.
   */
  private void migrateToVersion1() throws IOException {
    SonarPublisher.DescriptorImpl sonarDescriptor = Hudson.getInstance().getDescriptorByType(SonarPublisher.DescriptorImpl.class);
    SonarInstallation[] installations = sonarDescriptor.getInstallations();
    if (installations != null) {
      for (SonarInstallation installation : installations) {
        installation.setDatabasePassword(installation.getScrambledDatabasePassword());
        sonarDescriptor.save();
      }
    }
    configVersion = 1;
    save();
  }

}
