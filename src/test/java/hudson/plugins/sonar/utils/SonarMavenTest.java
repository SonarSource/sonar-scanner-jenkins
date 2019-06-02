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

import hudson.Launcher;
import hudson.maven.local_repo.DefaultLocalRepositoryLocator;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.plugins.sonar.SonarInstallation;
import hudson.plugins.sonar.SonarPublisher;
import hudson.util.ArgumentListBuilder;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SonarMavenTest {

  @Rule
  public JenkinsRule j = new JenkinsRule();

  @Test
  public void shouldWrapUpArguments() throws Exception {
    SonarPublisher publisher = mock(SonarPublisher.class);
    SonarInstallation installation = mock(SonarInstallation.class);
    when(installation.getServerUrl()).thenReturn("hostUrl");
    when(installation.getServerAuthenticationToken(any(Run.class))).thenReturn("xyz");
    when(publisher.getInstallation()).thenReturn(installation);
    when(publisher.getBranch()).thenReturn("branch");

    ArgumentListBuilder args = new ArgumentListBuilder();
    SonarMaven sonarMaven = new SonarMaven("-Dprop=value", "Default Maven", "pom.xml", "", new DefaultLocalRepositoryLocator(), publisher, mock(BuildListener.class), null, null,
      null);
    sonarMaven.wrapUpArguments(args, "sonar:sonar", mock(AbstractBuild.class), mock(Launcher.class), mock(BuildListener.class));

    List<String> result = args.toList();
    assertThat(result).contains("-Dprop=value");
    assertThat(result).contains("-Dsonar.host.url=hostUrl");
    assertThat(result).contains("-Dsonar.branch=branch");
    assertThat(result).contains("-Dsonar.login=xyz");
  }

  @Test
  public void shouldReturnTarget() {
    SonarInstallation installation = mock(SonarInstallation.class);
    when(installation.getMojoVersion())
      .thenReturn("")
      .thenReturn("1.0-beta-2");
    assertThat(SonarMaven.getTarget(installation)).isEqualTo("-e -B sonar:sonar");
    assertThat(SonarMaven.getTarget(installation)).isEqualTo("-e -B org.codehaus.mojo:sonar-maven-plugin:1.0-beta-2:sonar");
  }

}
