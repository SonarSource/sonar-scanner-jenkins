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

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.plugins.sonar.BuildSonarAction;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SonarUtilsTest {

  @Test
  public void shouldParseUrlInLogs() throws Exception {
    assertThat(SonarUtils.extractSonarProjectURLFromLogs(mockedBuild(""))).isNull();
    String log = "foo\n" +
      "[INFO] [16:36:31.386] ANALYSIS SUCCESSFUL, you can browse http://sonar:9000/dashboard/index/myproject:onbranch\n"
      + "bar";
    assertThat(SonarUtils.extractSonarProjectURLFromLogs(mockedBuild(log))).isEqualTo("http://sonar:9000/dashboard/index/myproject:onbranch");
  }

  @Test
  public void shouldGetLastSuccessfulBuildUrl() throws Exception {
    // Given
    AbstractProject<?, ?> project = mock(AbstractProject.class);

    // When
    when(project.getLastSuccessfulBuild()).thenReturn(null);

    // Then
    assertThat(SonarUtils.getLastSonarUrl(project)).isNull();
  }

  @Test
  public void getSuccessfulBuildWithValidUrl() throws Exception {
    // Given
    AbstractProject<?, ?> project = mock(AbstractProject.class);
    AbstractBuild build = mock(AbstractBuild.class);

    // When
    String expectedUrl = "http://foo";
    when(build.getAction(BuildSonarAction.class)).thenReturn(new BuildSonarAction(expectedUrl));
    when(project.getLastSuccessfulBuild()).thenReturn(build);

    // Then
    assertThat(SonarUtils.getLastSonarUrl(project)).isEqualTo(expectedUrl);
  }

  @Test
  public void successfulBuildWithoutSonarAction() throws Exception {
    // Given
    AbstractProject project = mock(AbstractProject.class);
    AbstractBuild build = mock(AbstractBuild.class);

    // When
    when(build.getAction(BuildSonarAction.class)).thenReturn(null);
    when(project.getLastSuccessfulBuild()).thenReturn(build);

    // Then
    assertThat(SonarUtils.getLastSonarUrl(project)).isNull();
  }

  @Test
  public void getUnstableBuildUrlWhenNoStableBuild() throws Exception {
    // Given
    AbstractProject<?, ?> project = mock(AbstractProject.class);
    AbstractBuild build = mock(AbstractBuild.class);

    // When
    when(project.getLastSuccessfulBuild()).thenReturn(null);

    String expectedUrl = "http://foo";
    when(build.getAction(BuildSonarAction.class)).thenReturn(new BuildSonarAction(expectedUrl));
    when(project.getLastUnstableBuild()).thenReturn(build);

    // Then
    assertThat(SonarUtils.getLastSonarUrl(project)).isEqualTo(expectedUrl);
  }

  @Test
  public void doNotUseUrlFromFailedBuild() throws Exception {
    // Given
    AbstractProject<?, ?> project = mock(AbstractProject.class);
    AbstractBuild build = mock(AbstractBuild.class);

    // When
    when(project.getLastSuccessfulBuild()).thenReturn(null);
    when(project.getLastUnstableBuild()).thenReturn(null);
    when(project.getLastFailedBuild()).thenReturn(build);

    // Then
    assertThat(SonarUtils.getLastSonarUrl(project)).isNull();
  }

  private AbstractBuild<?, ?> mockedBuild(String log) throws IOException {
    AbstractBuild<?, ?> build = mock(AbstractBuild.class);
    when(build.getLogReader()).thenReturn(new StringReader(log));
    return build;
  }

  private Run<?, ?> mockedRunWithSonarAction(String url) throws IOException {
    Run<?, ?> build = mock(Run.class);
    when(build.getAction(BuildSonarAction.class)).thenReturn(url != null ? new BuildSonarAction(url) : null);
    return build;
  }

}
