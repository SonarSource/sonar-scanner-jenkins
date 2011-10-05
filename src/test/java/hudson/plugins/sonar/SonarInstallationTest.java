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

import hudson.plugins.sonar.utils.MagicNames;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Evgeny Mandrikov
 */
public class SonarInstallationTest {
  @Test
  public void defaultUrl() throws Exception {
    SonarInstallation installation = create(null);
    assertThat(installation.getServerLink(), is(MagicNames.DEFAULT_SONAR_URL));

    installation = create(" ");
    assertThat(installation.getServerLink(), is(MagicNames.DEFAULT_SONAR_URL));
  }

  @Test
  public void publicUrl() {
    SonarInstallation installation = create("http://localhost");
    installation.setServerPublicUrl("http://host:port/sonar");
    assertThat(installation.getServerLink(), is("http://host:port/sonar"));
  }

  @Test
  public void trailingSlashAndSpace() throws Exception {
    SonarInstallation installation = create("http://host:port/sonar/ ");
    assertThat(installation.getServerLink(), is("http://host:port/sonar"));
  }

  @Test
  public void projectLink() {
    SonarInstallation installation = create("http://localhost/sonar");
    assertThat(
        installation.getProjectLink("org.example", "myproject", null),
        is("http://localhost/sonar/project/index/org.example:myproject")
    );
  }

  @Test
  public void projectLinkWithBranch() throws Exception {
    SonarInstallation installation = create("http://localhost/sonar");
    assertThat(
        installation.getProjectLink("org.example", "myproject", "1.2"),
        is("http://localhost/sonar/project/index/org.example:myproject:1.2")
    );
  }

  private SonarInstallation create(String url) {
    return new SonarInstallation("default", false, url, null, "", "", "", "", null, "", null);
  }
}
