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

import hudson.model.Result;
import hudson.model.Run;
import org.junit.Test;
import hudson.Functions;
import hudson.model.FreeStyleProject;
import hudson.util.jna.GNUCLibrary;

import java.io.File;
import java.net.URISyntaxException;

public class MsBuildSQRunnerEndTest extends SonarTestCase {
  @Test
  public void testNormalExec() throws Exception {
    configureDefaultSonar();
    configureMsBuildRunner(false);

    FreeStyleProject proj = setupFreeStyleProject(new MsBuildSQRunnerBegin("default", "default", "key", "name", "1.0", ""));
    proj.getBuildersList().add(new MsBuildSQRunnerEnd());
    Run<?, ?> r = build(proj, Result.SUCCESS);
    assertLogContains("MSBuild.SonarQube.Runner.exe end", r);
    assertLogContains("This is a fake MS Build Runner", r);
  }
  
  @Test
  public void NoBegin() throws Exception {
    configureDefaultSonar();
    configureMsBuildRunner(false);

    FreeStyleProject proj = setupFreeStyleProject(new MsBuildSQRunnerEnd());
    Run<?, ?> r = build(proj, Result.FAILURE);
    assertLogContains("No MSBuild SonarQube Runner installation found in the build environment", r);
  }

  private MsBuildSQRunnerInstallation configureMsBuildRunner(boolean fail) throws URISyntaxException {
    String res = "SonarTestCase/ms-build-runner";
    if(fail) {
      res += "-broken";
    }
    File home = new File(getClass().getResource(res).toURI().getPath());
    String exeName = null;

    if (Functions.isWindows()) {
      exeName = "MSBuild.SonarQube.Runner.bat";
    } else {
      exeName = "MSBuild.SonarQube.Runner.exe";
      GNUCLibrary.LIBC.chmod(new File(home, exeName).getAbsolutePath(), 0755);
    }
    MsBuildSQRunnerInstallation inst = new MsBuildSQRunnerInstallation("default", home.getAbsolutePath(), null, exeName);
    j.jenkins.getDescriptorByType(MsBuildSQRunnerInstallation.DescriptorImpl.class).setInstallations(inst);

    return inst;
  }
}
