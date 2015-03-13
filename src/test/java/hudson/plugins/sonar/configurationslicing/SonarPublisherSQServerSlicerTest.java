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
package hudson.plugins.sonar.configurationslicing;

import hudson.maven.MavenModuleSet;
import hudson.plugins.sonar.SonarPublisher;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SonarPublisherSQServerSlicerTest {

  @Rule
  public JenkinsRule j = new JenkinsRule();

  @Test
  public void availableMavenProjectsWithSonarPublisher() throws IOException {
    final MavenModuleSet project = j.createMavenProject();
    assertThat(new SonarPublisherSQServerSlicer().getWorkDomain().size()).isZero();
    project.getPublishersList().add(new SonarPublisher("MySonar", null, null, null, null, null, null, null, null, null, false));
    assertThat(new SonarPublisherSQServerSlicer().getWorkDomain().size()).isEqualTo(1);
  }

  @Test
  public void changeJobAdditionalProperties() throws Exception {
    final MavenModuleSet project = j.createMavenProject();
    final SonarPublisher mySonar = new SonarPublisher("MySonar", null, null, null, null, null, null, null, null, null, false);
    project.getPublishersList().add(mySonar);

    final SonarPublisherSQServerSlicer.SonarPublisherSQInstallSlicerSpec spec = new SonarPublisherSQServerSlicer.SonarPublisherSQInstallSlicerSpec();
    final List<String> values = spec.getValues(project);
    assertThat(values.get(0)).isEqualTo("MySonar");
    final List<String> newValues = new ArrayList<String>();
    newValues.add("MySonar 2");
    spec.setValues(project, newValues);

    assertThat(mySonar.getInstallationName()).isEqualTo("MySonar 2");
  }

}
