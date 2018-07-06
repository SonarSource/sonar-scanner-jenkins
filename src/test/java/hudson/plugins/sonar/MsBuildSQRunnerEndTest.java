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
package hudson.plugins.sonar;

import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.plugins.sonar.MsBuildSQRunnerEnd.DescriptorImpl;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class MsBuildSQRunnerEndTest extends MsBuildSQRunnerTest {

  @Test
  public void testToken() throws Exception {
    SonarInstallation inst = spy(new SonarInstallation(SONAR_INSTALLATION_NAME, "localhost", "credentialsId", null, null, null, null));
    when(inst.getServerAuthenticationToken(any(Run.class))).thenReturn("token");
    configureSonar(inst);
    configureMsBuildScanner(false);

    FreeStyleProject proj = setupFreeStyleProject(new MsBuildSQRunnerBegin("default", "default", "key", "name", "1.0", ""));
    proj.getBuildersList().add(new MsBuildSQRunnerEnd());
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
