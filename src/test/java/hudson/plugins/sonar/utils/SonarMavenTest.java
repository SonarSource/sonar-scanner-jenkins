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

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.plugins.sonar.SonarInstallation;
import hudson.plugins.sonar.SonarPublisher;
import hudson.util.ArgumentListBuilder;

import java.util.List;

import org.junit.Test;

public class SonarMavenTest {

  @Test
  public void shouldWrapUpArguments() throws Exception {
    SonarPublisher publisher = mock(SonarPublisher.class);
    SonarInstallation installation = mock(SonarInstallation.class);
    when(installation.getServerUrl()).thenReturn("hostUrl");
    when(installation.getDatabaseUrl()).thenReturn("databaseUrl");
    when(installation.getDatabaseDriver()).thenReturn("driver");
    when(installation.getDatabaseLogin()).thenReturn("login");
    when(installation.getDatabasePassword()).thenReturn("password");
    when(publisher.getInstallation()).thenReturn(installation);
    when(publisher.getBranch()).thenReturn("branch");
    when(publisher.getLanguage()).thenReturn("language");

    ArgumentListBuilder args = new ArgumentListBuilder();
    SonarMaven sonarMaven = new SonarMaven("-Dprop=value", "Default Maven", "pom.xml", "", false, publisher);
    sonarMaven.wrapUpArguments(args, "sonar:sonar", mock(AbstractBuild.class), mock(Launcher.class), mock(BuildListener.class));

    List<String> result = args.toList();
    assertThat(result, hasItem("-Dprop=value"));
    assertThat(result, hasItem("-Dsonar.jdbc.driver=driver"));
    assertThat(result, hasItem("-Dsonar.jdbc.url=databaseUrl"));
    assertThat(result, hasItem("-Dsonar.jdbc.username=login"));
    assertThat(result, hasItem("-Dsonar.jdbc.password=password"));
    assertThat(result, hasItem("-Dsonar.host.url=hostUrl"));
    assertThat(result, hasItem("-Dsonar.branch=branch"));
    assertThat(result, hasItem("-Dsonar.language=language"));
  }

  @Test
  public void shouldReturnTarget() {
    SonarInstallation installation = mock(SonarInstallation.class);
    when(installation.getMojoVersion())
        .thenReturn("")
        .thenReturn("1.0-beta-2");
    assertThat(SonarMaven.getTarget(installation), is("-e -B sonar:sonar"));
    assertThat(SonarMaven.getTarget(installation), is("-e -B org.codehaus.mojo:sonar-maven-plugin:1.0-beta-2:sonar"));
  }

}
