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

import hudson.Util;
import hudson.plugins.sonar.SonarPublisher.DescriptorImpl;
import hudson.plugins.sonar.model.TriggersConfig;
import jenkins.model.Jenkins;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Evgeny Mandrikov
 */
public class SonarInstallationTest extends SonarTestCase {

  @Test
  public void testRoundtrip() throws IOException {
    TriggersConfig triggers = new TriggersConfig();
    DescriptorImpl d = descriptor();
    d.setInstallations(new SonarInstallation(
      "Name",
      false,
      "server.url",
      "db:url",
      "dbLogin",
      "dbPasswd",
      "mojoVersion",
      "props",
      triggers,
      "sonarLogin",
      "sonarPasswd"
      ));
    d.save();

    SonarInstallation i = descriptor().getInstallations()[0];
    String storedConfig = Util.loadFile(new File(Jenkins.getInstance().getRootDir(), d.getId() + ".xml"));

    assertThat(i.getName()).isEqualTo("Name");
    assertThat(i.isDisabled()).isFalse();
    assertThat(i.getServerUrl()).isEqualTo("server.url");
    assertThat(i.getDatabaseUrl()).isEqualTo("db:url");
    assertThat(i.getDatabaseLogin()).isEqualTo("dbLogin");
    assertThat(i.getDatabasePassword()).isEqualTo("dbPasswd");
    assertThat(i.getMojoVersion()).isEqualTo("mojoVersion");
    assertThat(i.getAdditionalProperties()).isEqualTo("props");
    assertThat(i.getSonarLogin()).isEqualTo("sonarLogin");
    assertThat(i.getSonarPassword()).isEqualTo("sonarPasswd");

    assertThat(storedConfig).doesNotContain("dbPasswd");
    assertThat(storedConfig).doesNotContain("sonarPasswd");
  }

  private SonarPublisher.DescriptorImpl descriptor() {
    return new SonarPublisher.DescriptorImpl();
  }
}
