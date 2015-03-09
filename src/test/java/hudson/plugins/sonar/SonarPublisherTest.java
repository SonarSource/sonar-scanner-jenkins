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
package hudson.plugins.sonar;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SonarPublisherTest {

  @Test
  public void shouldMigrateOldLanguageProperty() {
    SonarPublisher publisher = new SonarPublisher("Foo", null, null, null, null, null, null, null, null, null, false);
    publisher.language = "js";
    publisher.readResolve();
    assertThat(publisher.getJobAdditionalProperties()).isEqualTo("-Dsonar.language=js");

    publisher = new SonarPublisher("Foo", null, null, "-Dsonar.version=1.0", null, null, null, null, null, null, false);
    publisher.language = "js";
    publisher.readResolve();
    assertThat(publisher.getJobAdditionalProperties()).isEqualTo("-Dsonar.language=js -Dsonar.version=1.0");
  }
}
