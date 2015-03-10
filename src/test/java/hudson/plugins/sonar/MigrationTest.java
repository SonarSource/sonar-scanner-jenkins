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

import org.jvnet.hudson.test.recipes.LocalData;

import static org.assertj.core.api.Assertions.assertThat;

public class MigrationTest extends SonarTestCase {

  /**
   * See SONARPLUGINS-470
   */
  @LocalData
  public void testShouldMigrateDatabasePasswords() {
    SonarInstallation installation;

    installation = SonarInstallation.get("Plaintext");
    assertThat(installation.getDatabasePassword()).isEqualTo("plainDbPasswd");
    assertThat(installation.getSonarPassword()).isEqualTo("plainSonarPasswd");

    installation = SonarInstallation.get("Scrambled");
    assertThat(installation.getDatabasePassword()).isEqualTo("scrambledDbPasswd");
    assertThat(installation.getSonarPassword()).isEqualTo("scrambledSonarPasswd");

    installation = SonarInstallation.get("Secret");
    assertThat(installation.getDatabasePassword()).isEqualTo("secretDbPasswd");
    assertThat(installation.getSonarPassword()).isEqualTo("secretSonarPasswd");
  }
}
