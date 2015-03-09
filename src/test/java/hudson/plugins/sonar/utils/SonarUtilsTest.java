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
import hudson.util.RunList;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;

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
  public void shouldGetLastUrl() throws Exception {
    AbstractProject<?, ?> project = mock(AbstractProject.class);
    Run<?, ?> build1 = mockedRunWithSonarAction(null);
    Run<?, ?> build2 = mockedRunWithSonarAction("http://foo");
    RunList list = mock(RunList.class);
    when(list.iterator()).thenReturn(Arrays.asList(build1, build2).iterator());
    when(project.getBuilds()).thenReturn(list);
    assertThat(SonarUtils.getLastSonarUrl(project)).isEqualTo("http://foo");
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
