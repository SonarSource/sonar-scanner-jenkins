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
package hudson.plugins.sonar.utils;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.plugins.sonar.action.SonarAnalysisAction;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SonarUtilsTest {
  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void shouldParseUrlInLogs() throws Exception {
    assertThat(SonarUtils.extractSonarProjectURLFromLogs(mockedBuild(""))).isNull();
    String log = "foo\n" +
      "[INFO] [16:36:31.386] ANALYSIS SUCCESSFUL, you can browse http://sonar:9000/dashboard/index/myproject:onbranch\n"
      + "bar";
    assertThat(SonarUtils.extractSonarProjectURLFromLogs(mockedBuild(log))).isEqualTo("http://sonar:9000/dashboard/index/myproject:onbranch");
  }

  @Test
  public void testExtractReport() throws Exception {
    File report = new File(temp.getRoot(), "report-task.txt");
    FileUtils.write(report, "key1=value1");

    assertThat(SonarUtils.extractReportTask(mockedBuild(""), new FilePath(temp.getRoot()))).isNull();
    String log1 = "foo\n" +
      "[INFO] [16:36:31.386] Working dir: /tmp/not_existing_dir\n"
      + "bar";
    String log2 = "foo\n" +
      "[INFO] [16:36:31.386] Working dir: " + temp.getRoot().getAbsolutePath() + "\n"
      + "bar";

    Properties props = new Properties();
    props.put("key1", "value1");
    assertThat(SonarUtils.extractReportTask(mockedBuild(log1), new FilePath(temp.getRoot()))).isNull();
    assertThat(SonarUtils.extractReportTask(mockedBuild(log2), new FilePath(temp.getRoot()))).isEqualTo(props);
  }

  @Test
  public void testMajorMinor() {
    assertThat(SonarUtils.extractMajorMinor("3.0")).isEqualTo(3.0f);
    assertThat(SonarUtils.extractMajorMinor("3.0.3")).isEqualTo(3.0f);
    assertThat(SonarUtils.extractMajorMinor("3.0-SNAPSHOT")).isEqualTo(3.0f);
    assertThat(SonarUtils.extractMajorMinor("30")).isEqualTo(null);
    assertThat(SonarUtils.extractMajorMinor("sdf")).isEqualTo(null);
  }

  @Test
  public void testMavenGoal() {
    assertThat(SonarUtils.getMavenGoal("3.0")).isEqualTo("org.sonarsource.scanner.maven:sonar-maven-plugin:3.0:sonar");
    assertThat(SonarUtils.getMavenGoal("2.5")).isEqualTo("org.codehaus.mojo:sonar-maven-plugin:2.5:sonar");

    exception.expect(NullPointerException.class);
    SonarUtils.getMavenGoal(null);
  }

  @Test
  public void testAddBuildInfoFromLastBuild() {
    SonarAnalysisAction a1 = new SonarAnalysisAction("inst");
    a1.setSkipped(true);
    a1.setUrl("url1");
    a1.setCeTaskId("task1");

    Run last = mockedRun(null, a1);
    Run r = mockedRun(last);

    SonarAnalysisAction action = SonarUtils.addBuildInfoFromLastBuildTo(r, "inst", false);

    assertThat(action.getInstallationName()).isEqualTo("inst");
    assertThat(action.isSkipped()).isFalse();
    assertThat(action.getCeTaskId()).isNull();
    assertThat(action.getUrl()).isEqualTo("url1");
  }

  private static Run mockedRun(Run previous, SonarAnalysisAction... actions) {
    Run r = mock(Run.class);
    when(r.getActions(SonarAnalysisAction.class)).thenReturn(Arrays.asList(actions));
    when(r.getPreviousBuild()).thenReturn(previous);
    return r;
  }

  private static AbstractBuild<?, ?> mockedBuild(String log) throws IOException {
    AbstractBuild<?, ?> build = mock(AbstractBuild.class);
    when(build.getLogReader()).thenReturn(new StringReader(log));
    return build;
  }

}
