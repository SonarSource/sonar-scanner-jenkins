/*
 * Jenkins :: Integration Tests
 * Copyright (C) 2013-2018 SonarSource SA
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

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.MavenLocation;
import java.io.File;
import javax.annotation.CheckForNull;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.sonarqube.ws.Components.Component;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.components.ShowRequest;

import static java.util.Objects.requireNonNull;

@RunWith(Suite.class)
@SuiteClasses({JenkinsTest.class, JenkinsWithoutMaven.class, JenkinsPipelineTest.class})
public class JenkinsTestSuite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .setSonarVersion(requireNonNull(System.getProperty("sonar.runtimeVersion"), "Please set system property sonar.runtimeVersion"))
    .addPlugin(MavenLocation.of("org.sonarsource.java","sonar-java-plugin", "LATEST_RELEASE"))
    .addPlugin(MavenLocation.of("org.sonarsource.javascript","sonar-javascript-plugin", "LATEST_RELEASE"))
    // Needed by Scanner for MSBuild
    .addPlugin(MavenLocation.of("org.sonarsource.dotnet","sonar-csharp-plugin", "LATEST_RELEASE"))
    .restoreProfileAtStartup(FileLocation.ofClasspath("/com/sonar/it/jenkins/JenkinsTest/sonar-way-it-profile_java.xml"))
    .build();

  static boolean isWindows() {
    return File.pathSeparatorChar == ';' || System.getProperty("os.name").startsWith("Windows");
  }

  @CheckForNull
  static Component getProject(String componentKey) {
    try {
      return newWsClient().components().show(new ShowRequest().setComponent(componentKey)).getComponent();
    } catch (HttpException e) {
      if (e.code() == 404) {
        return null;
      }
      throw new IllegalStateException(e);
    }
  }

  static WsClient newWsClient() {
    return WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
      .url(ORCHESTRATOR.getServer().getUrl())
      .build());
  }
}
