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

import java.io.File;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class MsBuildSQRunnerInstallationTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void getScannerToolPath_when_testExeName_not_null() {
    MsBuildSQRunnerInstallation.setTestExeName("foo.exe");

    assertThat(MsBuildSQRunnerInstallation.getScannerToolPath("foo")).isEqualTo("foo" + File.separator + "foo.exe");
  }

  @Test
  public void getScannerToolPath_when_scannerNet46_path_exists() throws IOException {
    // Arrange
    MsBuildSQRunnerInstallation.setTestExeName(null);
    File tempFile = temp.newFile(MsBuildSQRunnerInstallation.SCANNER_EXE_NAME);

    // Act + Assert
    assertThat(MsBuildSQRunnerInstallation.getScannerToolPath(temp.getRoot().getPath()))
      .isEqualTo(tempFile.getPath());
  }

  @Test
  public void getScannerToolPath_when_scannerNetCore_path_exists() throws IOException {
    // Arrange
    MsBuildSQRunnerInstallation.setTestExeName(null);
    File tempFile = temp.newFile(MsBuildSQRunnerInstallation.SCANNER_DLL_NAME);

    // Act + Assert
    assertThat(MsBuildSQRunnerInstallation.getScannerToolPath(temp.getRoot().getPath()))
      .isEqualTo(tempFile.getPath());
  }

  @Test
  public void getScannerToolPath_when_oldScanner_path_exists() throws IOException {
    // Arrange
    MsBuildSQRunnerInstallation.setTestExeName(null);
    File tempFile = temp.newFile(MsBuildSQRunnerInstallation.OLD_SCANNER_EXE_NAME);

    // Act + Assert
    assertThat(MsBuildSQRunnerInstallation.getScannerToolPath(temp.getRoot().getPath()))
      .isEqualTo(tempFile.getPath());
  }

  @Test
  public void getScannerToolPath_when_no_path_exists() throws IOException {
    // Arrange
    MsBuildSQRunnerInstallation.setTestExeName(null);

    // Act + Assert
    assertThat(MsBuildSQRunnerInstallation.getScannerToolPath(temp.newFolder().getPath()))
      .isEqualTo(null);
  }

  @Test
  public void getScannerName_when_testExeName_not_null() {
    // Arrange
    MsBuildSQRunnerInstallation.setTestExeName("foo");

    // Act + Assert
    assertThat(MsBuildSQRunnerInstallation.getScannerName()).isEqualTo("foo");
  }

  @Test
  public void getScannerName_when_testExeName_null() {
    // Arrange
    MsBuildSQRunnerInstallation.setTestExeName(null);

    // Act + Assert
    assertThat(MsBuildSQRunnerInstallation.getScannerName()).isEqualTo(MsBuildSQRunnerInstallation.SCANNER_EXE_NAME);
  }
}
