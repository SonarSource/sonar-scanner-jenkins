/*
 * SonarQube Scanner for Jenkins
 * Copyright (C) 2007-2023 SonarSource SA
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
package hudson.plugins.sonar;

import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.plugins.sonar.MsBuildSQRunnerEnd.DescriptorImpl;
import hudson.plugins.sonar.client.HttpClient;
import hudson.plugins.sonar.client.WsClient;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class MsBuildSQRunnerEndTest extends MsBuildSQRunnerTest {

  @Test
  public void testToken() throws Exception {
    testTokenForVersion("9.9");
  }

  @Test
  public void testTokenWithSonarToken() throws Exception {
    testTokenForVersion("10.0");
  }

  public void testTokenForVersion(String serverVersion) throws Exception {
    SonarInstallation inst = spy(new SonarInstallation(SONAR_INSTALLATION_NAME, "localhost", "credentialsId", null,null, null, null, null, null));
    addCredential("credentialsId", "token");
    configureSonar(inst);
    configureMsBuildScanner(false);

    HttpClient client = mock(HttpClient.class);
    when(client.getHttp(inst.getServerUrl() + WsClient.API_VERSION, null)).thenReturn(serverVersion);

    MsBuildSQRunnerBegin runnerBegin = new MsBuildSQRunnerBegin("default", "default", "key", "name", "1.0", "");
    runnerBegin.setClient(client);
    MsBuildSQRunnerEnd runnerEnd = new MsBuildSQRunnerEnd();
    runnerEnd.setClient(client);

    FreeStyleProject proj = setupFreeStyleProject(runnerBegin);
    proj.getBuildersList().add(runnerEnd);
    Run<?, ?> r = build(proj, Result.SUCCESS);
    assertLogContains("end ********", r);
    assertLogContains("This is a fake MS Build Scanner", r);

    assertLogDoesntContains("token", r);
    assertLogDoesntContains("login", r);
  }

  @Test
  public void testDesc() {
    DescriptorImpl desc = new MsBuildSQRunnerEnd.DescriptorImpl();
    assertThat(desc.getHelpFile()).isNotNull();
    assertThat(desc.isApplicable(AbstractProject.class)).isTrue();
  }

  @Test
  public void NoBegin() throws Exception {
    configureDefaultSonar();
    configureMsBuildScanner(false);

    FreeStyleProject proj = setupFreeStyleProject(new MsBuildSQRunnerEnd());
    Run<?, ?> r = build(proj, Result.FAILURE);
    assertLogContains("Missing parameters in the build environment. Was the begin step invoked before?", r);
  }
}
