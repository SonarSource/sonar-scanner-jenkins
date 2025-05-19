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

import hudson.model.FreeStyleProject;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.hudson.test.recipes.LocalData;

import static org.assertj.core.api.Assertions.assertThat;

@WithJenkins
class GlobalCredentialMigratorTest extends SonarTestCase {

  private FreeStyleProject project;

  @Override
  @BeforeEach
  protected void setUp(JenkinsRule rule) throws Exception {
    super.setUp(rule);
    project = j.getInstance().createProject(FreeStyleProject.class, "test-sonar-cloud");
    project.scheduleBuild2(0).get();
  }

  @Test
  @LocalData
  void authTokenIsMigratedToCredential() {
    SonarInstallation sonarInstallation = SonarGlobalConfiguration.get().getInstallations()[0];
    StringCredentials authenticationToken = sonarInstallation.getCredentials(project.getFirstBuild());

    assertThat(authenticationToken.getSecret().getPlainText()).isEqualTo("fake-api-token");
    assertThat(authenticationToken.getDescription()).isEqualTo("Migrated SonarQube authentication token");
  }

  @Test
  @LocalData
  void existingCredentialIsReused() {
    SonarInstallation sonarInstallation = SonarGlobalConfiguration.get().getInstallations()[0];
    StringCredentials authenticationToken = sonarInstallation.getCredentials(project.getFirstBuild());

    assertThat(authenticationToken.getSecret().getPlainText()).isEqualTo("fake-api-token");
    assertThat(authenticationToken.getDescription()).isEqualTo("Pre-existing token");
    assertThat(authenticationToken.getId()).isEqualTo("test-api-token");
  }

  @Test
  @LocalData
  void authTokenIsMigratedToCredentialWhenSecretIsNull() {
    SonarInstallation sonarInstallation = SonarGlobalConfiguration.get().getInstallations()[0];
    StringCredentials authenticationToken = sonarInstallation.getCredentials(project.getFirstBuild());

    assertThat(authenticationToken.getSecret().getPlainText()).isEqualTo("fake-api-token");
    assertThat(authenticationToken.getDescription()).isEqualTo("Migrated SonarQube authentication token");
  }
}
