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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import org.jvnet.hudson.test.recipes.LocalData;

public class MigrationTest extends SonarTestCase {

  /**
   * See SONARPLUGINS-470
   */
  @LocalData
  public void testShouldMigrateDatabasePasswords() {
    SonarInstallation installation;

    installation = SonarInstallation.get("Server1");
    assertThat(installation.getDatabasePassword(), equalTo("secret1"));
    assertThat(installation.getScrambledDatabasePassword(), not(equalTo("secret1")));

    installation = SonarInstallation.get("Server2");
    assertThat(installation.getDatabasePassword(), equalTo("secret2"));
    assertThat(installation.getScrambledDatabasePassword(), not(equalTo("secret2")));
  }

}
