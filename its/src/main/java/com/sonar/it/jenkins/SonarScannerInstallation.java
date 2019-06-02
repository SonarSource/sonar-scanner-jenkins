/*
 * Jenkins :: Integration Tests
 * Copyright (C) 2013-2019 SonarSource SA
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
package com.sonar.it.jenkins;

import org.jenkinsci.test.acceptance.po.Jenkins;
import org.jenkinsci.test.acceptance.po.ToolInstallation;
import org.jenkinsci.test.acceptance.po.ToolInstallationPageObject;

@ToolInstallationPageObject(installer = "hudson.plugins.sonar.SonarRunnerInstaller", name = "SonarQube Scanner")
public class SonarScannerInstallation extends ToolInstallation {

  public SonarScannerInstallation(Jenkins jenkins, String path) {
    super(jenkins, path);
  }

  public static void install(final Jenkins jenkins, final String version) {
    jenkins.getPluginManager().checkForUpdates();
    installTool(jenkins, SonarScannerInstallation.class, getInstallName(version), version);
  }

  public static String getInstallName(final String version) {
    return "SonarQube Scanner " + version;
  }

}
