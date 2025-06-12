/*
 * SonarQube Scanner for Jenkins
 * Copyright (C) 2007-2025 SonarSource SA
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

import hudson.Functions;
import hudson.util.jna.GNUCLibrary;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.File;

@WithJenkins
abstract class MsBuildSQRunnerTest extends SonarTestCase {

  protected String getTestExeName() {
    if (isWindows()) {
      return "FakeWindowsMSBuildScanner.bat";
    } else {
      return "FakeUnixMSBuildScanner.sh";
    }
  }

  protected boolean isWindows() {
    return Functions.isWindows() || System.getProperty("os.name").startsWith("Windows");
  }

  protected MsBuildSQRunnerInstallation configureMsBuildScanner(boolean fail) throws Exception {
    String res = "SonarTestCase/ms-build-scanner";
    if (fail) {
      res += "-broken";
    }
    File home = new File(getClass().getResource(res).toURI().getPath());
    String testExeName = getTestExeName();

    if (!isWindows()) {
      GNUCLibrary.LIBC.chmod(new File(home, testExeName).getAbsolutePath(), 0755);
    }
    MsBuildSQRunnerInstallation inst = new MsBuildSQRunnerInstallation("default", home.getAbsolutePath(), null);
    MsBuildSQRunnerInstallation.setTestExeName(testExeName);
    j.jenkins.getDescriptorByType(MsBuildSQRunnerInstallation.DescriptorImpl.class).setInstallations(inst);

    return inst;
  }
}
