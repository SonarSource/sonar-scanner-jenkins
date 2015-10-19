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

import org.mockito.ArgumentCaptor;

import hudson.plugins.sonar.action.UrlSonarAction;
import hudson.plugins.sonar.action.BuildSonarAction;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;

import static org.mockito.Mockito.verify;
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
  public void addUrlActionWhenNoLogAndNoLastUrl() throws IOException {
    AbstractBuild build = mockedBuild("");

    UrlSonarAction action = SonarUtils.addUrlActionTo(build);
    assertThat(action).isNull();
  }

  @Test
  public void addUrlActionWhenNoLog() throws IOException {
    UrlSonarAction previousAction = new UrlSonarAction("url", true);
    AbstractBuild previousBuild = mock(AbstractBuild.class);
    when(previousBuild.getAction(UrlSonarAction.class)).thenReturn(previousAction);

    AbstractBuild build = mockedBuild("");
    when(build.getPreviousBuild()).thenReturn(previousBuild);

    UrlSonarAction action = SonarUtils.addUrlActionTo(build);

    ArgumentCaptor<UrlSonarAction> arg = ArgumentCaptor.forClass(UrlSonarAction.class);
    verify(build).addAction(arg.capture());

    assertThat(action.isNew()).isFalse();
    assertThat(action.getSonarUrl()).isEqualTo("url");

    assertThat(arg.getValue().getSonarUrl()).isEqualTo("url");
  }

  @Test
  public void addUrlActionWhenLog() throws IOException {
    String log = "foo\n" +
      "[INFO] [16:36:31.386] ANALYSIS SUCCESSFUL, you can browse http://sonar:9000/dashboard/index/myproject:onbranch\n"
      + "bar";
    AbstractBuild build = mockedBuild(log);
    UrlSonarAction action = SonarUtils.addUrlActionTo(build);

    ArgumentCaptor<UrlSonarAction> arg = ArgumentCaptor.forClass(UrlSonarAction.class);
    verify(build).addAction(arg.capture());

    assertThat(action.isNew()).isTrue();
    assertThat(action.getSonarUrl()).isEqualTo("http://sonar:9000/dashboard/index/myproject:onbranch");

    assertThat(arg.getValue().getSonarUrl()).isEqualTo("http://sonar:9000/dashboard/index/myproject:onbranch");
  }

  @Test
  public void getUrlFromNullBuild() {
    assertThat(SonarUtils.getSonarUrlFrom(null)).isNull();
  }

  @Test
  public void getUrlFromBuild() {
    AbstractBuild build = mock(AbstractBuild.class);
    UrlSonarAction action = new UrlSonarAction("url", true);
    when(build.getAction(UrlSonarAction.class)).thenReturn(action);
    assertThat(SonarUtils.getSonarUrlFrom(build)).isEqualTo("url");
  }

  @Test
  public void getUrlFromBuildWithoutAction() {
    AbstractBuild build = mock(AbstractBuild.class);
    assertThat(SonarUtils.getSonarUrlFrom(build)).isNull();
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
