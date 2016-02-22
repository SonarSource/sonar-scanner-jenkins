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

import hudson.plugins.sonar.action.SonarBuildBadgeAction;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SonarUtilsTest {
  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void shouldParseUrlInLogs() throws Exception {
    assertThat(SonarUtils.extractSonarProjectURLFromLogs(mockedBuild(""))).isNull();
    String log = "foo\n" +
      "[INFO] [16:36:31.386] ANALYSIS SUCCESSFUL, you can browse http://sonar:9000/dashboard/index/myproject:onbranch\n"
      + "bar";
    assertThat(SonarUtils.extractSonarProjectURLFromLogs(mockedBuild(log))).isEqualTo("http://sonar:9000/dashboard/index/myproject:onbranch");
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

  private AbstractBuild<?, ?> mockedBuild(String log) throws IOException {
    AbstractBuild<?, ?> build = mock(AbstractBuild.class);
    when(build.getLogReader()).thenReturn(new StringReader(log));
    return build;
  }

  private Run<?, ?> mockedRunWithSonarAction(String url) throws IOException {
    Run<?, ?> build = mock(Run.class);
    when(build.getAction(SonarBuildBadgeAction.class)).thenReturn(url != null ? new SonarBuildBadgeAction(url) : null);
    return build;
  }

}
