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

import javax.annotation.Nullable;

import hudson.model.EnvironmentContributingAction;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.FreeStyleProject;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MsBuildSQRunnerBeginTest extends MsBuildSQRunnerTest {

  @Test
  public void testNormalExec() throws Exception {
    configureDefaultSonar();
    configureMsBuildRunner(false);

    FreeStyleProject proj = createFreeStyleProjectWithMSBuild("default", "default");
    Run<?, ?> r = build(proj, Result.SUCCESS);

    assertLogContains(getExeName() + " begin /k:key /n:name /v:1.0", r);
    assertLogContains("This is a fake MS Build Runner", r);
    assertThat(r.getAction(EnvironmentContributingAction.class)).isNotNull();
  }

  @Test
  public void failExe() throws Exception {
    configureDefaultSonar();
    configureMsBuildRunner(true);

    FreeStyleProject proj = createFreeStyleProjectWithMSBuild("default", "default");
    Run<?, ?> r = build(proj, Result.FAILURE);

    assertLogContains(getExeName() + " begin /k:key /n:name /v:1.0", r);
    assertLogContains("This is a fake MS Build Runner", r);
  }

  @Test
  public void additionalArgs() throws Exception {
    SonarInstallation inst = new SonarInstallation("default", false, null, null, null, null,
      null, "/x:a=b", null, null, null, "key=value");
    configureSonar(inst);
    configureMsBuildRunner(true);

    FreeStyleProject proj = createFreeStyleProjectWithMSBuild("default", "default", "/y");
    Run<?, ?> r = build(proj, Result.FAILURE);

    assertLogContains(getExeName() + " begin /k:key /n:name /v:1.0 /d:key=value /x:a=b /y", r);
    assertLogContains("This is a fake MS Build Runner", r);
  }

  @Test
  public void testSonarProps() throws Exception {
    SonarInstallation inst = new SonarInstallation("default", false, "http://dummy-server:9090", null, null, null,
      null, null, null, "login", "mypass", null);
    configureSonar(inst);
    configureMsBuildRunner(false);

    FreeStyleProject proj = createFreeStyleProjectWithMSBuild("default", "default");
    Run<?, ?> r = build(proj, Result.SUCCESS);
    assertLogContains(getExeName() + " begin /k:key /n:name /v:1.0"
      + " /d:sonar.host.url=http://dummy-server:9090 /d:sonar.login=login ********", r);
    assertLogContains("This is a fake MS Build Runner", r);
    assertLogDoesntContains("mypass", r);
  }

  @Test
  public void testNoMsBuildInst() throws Exception {
    configureDefaultSonar();
    configureMsBuildRunner(false);

    FreeStyleProject proj = createFreeStyleProjectWithMSBuild("default", "non-existing");
    Run<?, ?> r = build(proj, Result.FAILURE);
    assertLogContains("No MSBuild SonarQube Runner installation found", r);
  }

  private FreeStyleProject createFreeStyleProjectWithMSBuild(String sonarInst, String msBuildInst) throws Exception {
    return createFreeStyleProjectWithMSBuild(sonarInst, msBuildInst, null);
  }

  private FreeStyleProject createFreeStyleProjectWithMSBuild(String sonarInst, String msBuildInst, @Nullable String additionalArgs) throws Exception {
    return setupFreeStyleProject(new MsBuildSQRunnerBegin(msBuildInst, sonarInst, "key", "name", "1.0", additionalArgs));
  }
}
