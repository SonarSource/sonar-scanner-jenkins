/*
 * SonarQube Scanner for Jenkins
 * Copyright (C) 2007-2025 SonarSource Sàrl
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

import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

class MsBuildSQRunnerInstallationTest {

  @TempDir
  private File temp;

  @Test
  void getScannerToolPath_when_testExeName_not_null() {
    MsBuildSQRunnerInstallation.setTestExeName("foo.exe");

    assertThat(MsBuildSQRunnerInstallation.getScannerToolPath("foo")).isEqualTo("foo" + File.separator + "foo.exe");
  }

  @Test
  void getScannerToolPath_when_scannerNet46_path_exists() throws Exception {
    // Arrange
    MsBuildSQRunnerInstallation.setTestExeName(null);
    File tempFile = new File(temp, MsBuildSQRunnerInstallation.SCANNER_EXE_NAME);
    assertThat(tempFile.createNewFile()).isTrue();

    // Act + Assert
    assertThat(MsBuildSQRunnerInstallation.getScannerToolPath(temp.getPath()))
            .isEqualTo(tempFile.getPath());
  }

  @Test
  void getScannerToolPath_when_scannerNetCore_path_exists() throws Exception {
    // Arrange
    MsBuildSQRunnerInstallation.setTestExeName(null);
    File tempFile = new File(temp, MsBuildSQRunnerInstallation.SCANNER_DLL_NAME);
    assertThat(tempFile.createNewFile()).isTrue();

    // Act + Assert
    assertThat(MsBuildSQRunnerInstallation.getScannerToolPath(temp.getPath()))
            .isEqualTo(tempFile.getPath());
  }

  @Test
  void getScannerToolPath_when_oldScanner_path_exists() throws Exception {
    // Arrange
    MsBuildSQRunnerInstallation.setTestExeName(null);
    File tempFile = new File(temp, MsBuildSQRunnerInstallation.OLD_SCANNER_EXE_NAME);
    assertThat(tempFile.createNewFile()).isTrue();

    // Act + Assert
    assertThat(MsBuildSQRunnerInstallation.getScannerToolPath(temp.getPath()))
            .isEqualTo(tempFile.getPath());
  }

  @Test
  void getScannerToolPath_when_no_path_exists() throws Exception {
    // Arrange
    MsBuildSQRunnerInstallation.setTestExeName(null);

    // Act + Assert
    assertThat(MsBuildSQRunnerInstallation.getScannerToolPath(
        Files.createDirectories(new File(temp, "junit").toPath()).toString()))
            .isNull();
  }

  @Test
  void getScannerName_when_testExeName_not_null() {
    // Arrange
    MsBuildSQRunnerInstallation.setTestExeName("foo");

    // Act + Assert
    assertThat(MsBuildSQRunnerInstallation.getScannerName()).isEqualTo("foo");
  }

  @Test
  void getScannerName_when_testExeName_null() {
    // Arrange
    MsBuildSQRunnerInstallation.setTestExeName(null);

    // Act + Assert
    assertThat(MsBuildSQRunnerInstallation.getScannerName()).isEqualTo(MsBuildSQRunnerInstallation.SCANNER_EXE_NAME);
  }

}
