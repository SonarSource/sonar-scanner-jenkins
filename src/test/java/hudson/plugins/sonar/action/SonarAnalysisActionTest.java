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
package hudson.plugins.sonar.action;

import org.junit.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class SonarAnalysisActionTest {

  @Test
  public void testRoundTrips() {
    SonarAnalysisAction analysis = new SonarAnalysisAction("inst");
    Properties props = new Properties();
    analysis.setUrl("url1");
    analysis.setNew(false);
    analysis.setReportTask(props);

    assertThat(analysis.getReportTask()).isEqualTo(props);
    assertThat(analysis.getUrl()).isEqualTo("url1");
    assertThat(analysis.getInstallationName()).isEqualTo("inst");
    assertThat(analysis.isNew()).isFalse();
  }

  @Test
  public void testCopyConstructor() {
    SonarAnalysisAction analysis = new SonarAnalysisAction("inst");
    Properties props = new Properties();
    analysis.setUrl("url1");
    analysis.setNew(true);
    analysis.setReportTask(props);

    SonarAnalysisAction analysis2 = new SonarAnalysisAction(analysis);
    assertThat(analysis2.getReportTask()).isEqualTo(props);
    assertThat(analysis2.getUrl()).isEqualTo("url1");
    assertThat(analysis2.getInstallationName()).isEqualTo("inst");
    assertThat(analysis2.isNew()).isFalse();
  }
}
