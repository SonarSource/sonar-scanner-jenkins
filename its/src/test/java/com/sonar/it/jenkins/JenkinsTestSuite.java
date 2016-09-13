/*
 * Jenkins :: Integration Tests
 * Copyright (C) 2013-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.locator.FileLocation;
import java.io.File;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({JenkinsTest.class, JenkinsWithoutMaven.class})
public class JenkinsTestSuite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    // TODO Java projects should be replaced by Xoo projects
    .setOrchestratorProperty("javaVersion", "LATEST_RELEASE")
    .addPlugin("java")
    // Needed by Scanner for MSBuild
    .setOrchestratorProperty("csharpVersion", "LATEST_RELEASE")
    .addPlugin("csharp")
    .restoreProfileAtStartup(FileLocation.ofClasspath("/com/sonar/it/jenkins/JenkinsTest/sonar-way-it-profile_java.xml"))
    .build();

  static boolean isWindows() {
    return File.pathSeparatorChar == ';' || System.getProperty("os.name").startsWith("Windows");
  }
}
