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
package hudson.plugins.sonar.client;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectInformationTest {
  @Test
  public void testRoundTrips() {
    ProjectInformation proj = new ProjectInformation("key");
    proj.setName("name");
    proj.setStatus("status");
    proj.setUrl("url");
    String[] errors = {"error1", "error2", "error3"};
    proj.setErrors(errors);
    proj.setCeUrl("ceUrl");
    proj.setCeStatus("ceStatus");

    assertThat(proj.getCeStatus()).isEqualTo("cestatus");
    assertThat(proj.getCeUrl()).isEqualTo("ceUrl");
    assertThat(proj.getErrors()).contains(errors);
    assertThat(proj.getUrl()).isEqualTo("url");
    assertThat(proj.getProjectKey()).isEqualTo("key");
    assertThat(proj.getProjectName()).isEqualTo("name");
    assertThat(proj.hasErrors()).isTrue();
  }
}
