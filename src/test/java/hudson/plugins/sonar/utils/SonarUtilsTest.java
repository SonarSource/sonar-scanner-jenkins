/*
 * SonarQube Scanner for Jenkins
 * Copyright (C) 2007-2021 SonarSource SA
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

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.sonar.SonarInstallation;
import hudson.plugins.sonar.action.SonarAnalysisAction;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
  public void testAddBuildInfoFromLastBuild() throws Exception {
    SonarAnalysisAction a1 = new SonarAnalysisAction("inst", "credId", null);
    a1.setSkipped(true);
    a1.setUrl("url1");
    a1.setCeTaskId("task1");

    Run last = mockedRun(null, a1);
    Run r = mockedRun(last);

    SonarInstallation sonarInstallation = new SonarInstallation("inst", null, "credentialsId", null, null, null, null, null, null);
    SonarAnalysisAction action = SonarUtils.addBuildInfoFromLastBuildTo(r, mock(TaskListener.class), sonarInstallation, "credId", false);

    assertThat(action.getInstallationName()).isEqualTo("inst");
    assertThat(action.getCredentialsId()).isEqualTo("credId");
    assertThat(action.isSkipped()).isFalse();
    assertThat(action.getCeTaskId()).isNull();
    assertThat(action.getUrl()).isEqualTo("url1");
  }

  @Test
  public void should_mark_build_as_unstable_when_java_warning_is_logged() throws Exception {
    Run r = mockedBuild("The version of Java (1.8.0_101) you have used to run this analysis is deprecated and we will stop accepting it from October 2020. Please update to at least Java 11.");
    FilePath workspace = new FilePath(new File("non_existing_file"));
    SonarInstallation sonarInstallation = new SonarInstallation("inst", "https://url.com", "credentialsId", null, null, null, null, null, null);
    TaskListener listener = mock(TaskListener.class);
    PrintStream printStream = mock(PrintStream.class);
    when(listener.getLogger()).thenReturn(printStream);

    SonarUtils.addBuildInfoTo(r, listener, workspace, sonarInstallation, "credId", false);

    verify(r).setResult(eq(Result.UNSTABLE));
    verify(printStream, times(1)).println("Pipeline marked as 'UNSTABLE'. Please update to at least Java 11. Find more information here on how to do this: https://sonarcloud.io/documentation/appendices/move-analysis-java-11/");
  }

  @Test
  public void should_not_mark_build_as_unstable_when_result_is_already_failed() throws Exception {
    Run r = mockedBuild("The version of Java (1.8.0_101) you have used to run this analysis is deprecated and we will stop accepting it from October 2020. Please update to at least Java 11.");
    FilePath workspace = new FilePath(new File("non_existing_file"));
    SonarInstallation sonarInstallation = new SonarInstallation("inst", "https://url.com", "credentialsId", null, null, null, null, null, null);
    TaskListener listener = mock(TaskListener.class);
    PrintStream printStream = mock(PrintStream.class);
    when(listener.getLogger()).thenReturn(printStream);

    when(r.getResult()).thenReturn(Result.FAILURE);
    SonarUtils.addBuildInfoTo(r, listener, workspace, sonarInstallation, "credId", false);

    verify(r, never()).setResult(eq(Result.UNSTABLE));
    verify(printStream, never()).println("Pipeline marked as 'UNSTABLE'. Please update to at least Java 11. Find more information here on how to do this: https://sonarcloud.io/documentation/appendices/move-analysis-java-11/");
  }

  private static Run mockedRun(Run previous, SonarAnalysisAction... actions) {
    Run r = mock(Run.class);
    when(r.getActions(SonarAnalysisAction.class)).thenReturn(Arrays.asList(actions));
    when(r.getPreviousBuild()).thenReturn(previous);
    return r;
  }

  private static AbstractBuild<?, ?> mockedBuild(String log) throws IOException, InterruptedException {
    AbstractBuild<?, ?> build = mock(AbstractBuild.class);
    when(build.getLogReader()).thenReturn(new StringReader(log));
    when(build.getLog(anyInt())).thenReturn(Collections.singletonList(log));
    when(build.getBuildVariables()).thenReturn(Collections.emptyMap());
    when(build.getEnvironment(any())).thenReturn(new EnvVars());
    return build;
  }

}
