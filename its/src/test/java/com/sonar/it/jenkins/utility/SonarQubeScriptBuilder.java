/*
 * Jenkins :: Integration Tests
 * Copyright (C) 2013-2022 SonarSource SA
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
package com.sonar.it.jenkins.utility;

import java.nio.file.Paths;
import java.util.Objects;
import org.apache.commons.lang3.SystemUtils;

import static com.sonar.it.jenkins.utility.JenkinsUtils.DEFAULT_SONARQUBE_INSTALLATION;
import static java.util.regex.Matcher.quoteReplacement;

public class SonarQubeScriptBuilder {

  private String sonarQubeScannerVersion;

  private SonarQubeScriptBuilder() {
    //nothing to do
  }

  public static SonarQubeScriptBuilder newScript() {
    return new SonarQubeScriptBuilder();
  }

  public SonarQubeScriptBuilder sonarQubeScannerVersion(String sonarQubeScannerVersion) {
    this.sonarQubeScannerVersion = sonarQubeScannerVersion;
    return this;
  }

  public String build() {
    Objects.requireNonNull(sonarQubeScannerVersion);
    StringBuilder script = new StringBuilder();
    script
      .append("withSonarQubeEnv('")
      .append(DEFAULT_SONARQUBE_INSTALLATION)
      .append("') {\n");
    if (SystemUtils.IS_OS_WINDOWS) {
      script
        .append("  bat 'xcopy ")
        .append(Paths.get("projects/js").toAbsolutePath().toString().replaceAll("\\\\", quoteReplacement("\\\\")))
        .append(" . /s /e /y'\n");
    } else {
      script
        .append("  sh 'cp -rf ")
        .append(Paths.get("projects/js").toAbsolutePath())
        .append("/. .'\n");
    }
    script
      .append("  def scannerHome = tool 'SonarQube Scanner ")
      .append(sonarQubeScannerVersion)
      .append("'\n");
    if (SystemUtils.IS_OS_WINDOWS) {
      script.append("  bat \"${scannerHome}\\\\bin\\\\sonar-scanner.bat\"\n");
    } else {
      script.append("  sh \"${scannerHome}/bin/sonar-scanner\"\n");
    }
    script.append("}\n")
      .append("def qg = waitForQualityGate()\n")
      .append("if (qg.status != 'OK') { error 'Quality gate failure'}\n");
    return script.toString();
  }
}
