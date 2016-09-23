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
package hudson.plugins.sonar;

import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.plugins.sonar.MsBuildSQRunnerEnd.DescriptorImpl;
import hudson.plugins.sonar.utils.SQServerVersions;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MsBuildSQRunnerEndTest extends MsBuildSQRunnerTest {
  @Test
  public void testNormalExec() throws Exception {
    configureSonar(new SonarInstallation(SONAR_INSTALLATION_NAME, "localhost", SQServerVersions.SQ_5_1_OR_LOWER, null, "http://dbhost.org", "dbLogin", "dbPass", null, null, null,
      "login", "mypass", null));
    configureMsBuildScanner(false);

    FreeStyleProject proj = setupFreeStyleProject(new MsBuildSQRunnerBegin("default", "default", "key", "name", "1.0", ""));
    proj.getBuildersList().add(new MsBuildSQRunnerEnd());
    Run<?, ?> r = build(proj, Result.SUCCESS);
    assertLogContains("end /d:sonar.login=login ******** /d:sonar.jdbc.username=dbLogin ********", r);
    assertLogContains("This is a fake MS Build Scanner", r);

    assertLogDoesntContains("dbPass", r);
    assertLogDoesntContains("mypass", r);
  }

  @Test
  public void testToken() throws Exception {
    configureSonar(new SonarInstallation(SONAR_INSTALLATION_NAME, "localhost", SQServerVersions.SQ_5_3_OR_HIGHER, "token", null, null, null, null, null, null, null, null, null));
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
