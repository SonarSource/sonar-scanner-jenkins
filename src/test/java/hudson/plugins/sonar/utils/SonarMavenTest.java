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

import hudson.Launcher;
import hudson.maven.local_repo.DefaultLocalRepositoryLocator;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.plugins.sonar.SonarInstallation;
import hudson.plugins.sonar.SonarPublisher;
import hudson.util.ArgumentListBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
    when(installation.getDatabaseUrl()).thenReturn("databaseUrl");
    when(installation.getDatabaseLogin()).thenReturn("login");
    when(installation.getDatabasePassword()).thenReturn("password");
    when(installation.getSonarLogin()).thenReturn("sonarlogin");
    when(installation.getSonarPassword()).thenReturn("sonarpassword");
    when(publisher.getInstallation()).thenReturn(installation);
    when(publisher.getBranch()).thenReturn("branch");

    ArgumentListBuilder args = new ArgumentListBuilder();
    SonarMaven sonarMaven = new SonarMaven("-Dprop=value", "Default Maven", "pom.xml", "", new DefaultLocalRepositoryLocator(), publisher, mock(BuildListener.class), null, null,
      null);
    sonarMaven.wrapUpArguments(args, "sonar:sonar", mock(AbstractBuild.class), mock(Launcher.class), mock(BuildListener.class));

    List<String> result = args.toList();
    assertThat(result).contains("-Dprop=value");
    assertThat(result).contains("-Dsonar.jdbc.url=databaseUrl");
    assertThat(result).contains("-Dsonar.jdbc.username=login");
    assertThat(result).contains("-Dsonar.jdbc.password=password");
    assertThat(result).contains("-Dsonar.host.url=hostUrl");
    assertThat(result).contains("-Dsonar.branch=branch");
    assertThat(result).contains("-Dsonar.login=sonarlogin");
    assertThat(result).contains("-Dsonar.password=sonarpassword");
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
