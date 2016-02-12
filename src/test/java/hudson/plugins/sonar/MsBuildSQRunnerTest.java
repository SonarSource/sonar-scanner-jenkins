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

import hudson.Functions;
import hudson.util.jna.GNUCLibrary;

import java.io.File;
import java.net.URISyntaxException;

public abstract class MsBuildSQRunnerTest extends SonarTestCase {
  protected String getExeName() {
    if (isWindows()) {
      return "MSBuild.SonarQube.Runner.bat";
    } else {
      return "MSBuild.SonarQube.Runner.exe";
    }
  }

  protected boolean isWindows() {
    return Functions.isWindows() || System.getProperty("os.name").startsWith("Windows");
  }

  protected MsBuildSQRunnerInstallation configureMsBuildScanner(boolean fail) throws URISyntaxException {
    String res = "SonarTestCase/ms-build-scanner";
    if (fail) {
      res += "-broken";
    }
    File home = new File(getClass().getResource(res).toURI().getPath());
    String exeName = getExeName();

    if (!isWindows()) {
      GNUCLibrary.LIBC.chmod(new File(home, exeName).getAbsolutePath(), 0755);
    }
    MsBuildSQRunnerInstallation inst = new MsBuildSQRunnerInstallation("default", home.getAbsolutePath(), null);
    MsBuildSQRunnerInstallation.setExeName(exeName);
    j.jenkins.getDescriptorByType(MsBuildSQRunnerInstallation.DescriptorImpl.class).setInstallations(inst);

    return inst;
  }
}
