/*
 * SonarQube Scanner for Jenkins
 * Copyright (C) 2007-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package hudson.plugins.sonar.utils;

import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.plugins.sonar.action.SonarAnalysisAction;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SonarUtilsTest {
  @Rule
  public ExpectedException exception = ExpectedException.none();

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
    SonarAnalysisAction a1 = new SonarAnalysisAction("inst", "credId");
    a1.setSkipped(true);
    a1.setUrl("url1");
    a1.setCeTaskId("task1");

    Run last = mockedRun(null, a1);
    Run r = mockedRun(last);

    SonarAnalysisAction action = SonarUtils.addBuildInfoFromLastBuildTo(r, "inst", "credId", false);

    assertThat(action.getInstallationName()).isEqualTo("inst");
    assertThat(action.getCredentialsId()).isEqualTo("credId");
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
