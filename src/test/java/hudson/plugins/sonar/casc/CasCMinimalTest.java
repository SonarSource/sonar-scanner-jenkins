/*
 * SonarQube Scanner for Jenkins
 * Copyright (C) 2007-2021 SonarSource SA
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
package hudson.plugins.sonar.casc;

import hudson.plugins.sonar.SonarGlobalConfiguration;
import hudson.plugins.sonar.SonarInstallation;
import io.jenkins.plugins.casc.misc.RoundTripAbstractTest;
import jenkins.model.GlobalConfiguration;
import org.jvnet.hudson.test.RestartableJenkinsRule;

import static org.assertj.core.api.Assertions.assertThat;

public class CasCMinimalTest extends RoundTripAbstractTest {

  @Override
  protected String configResource() {
    return "config-minimal.yaml";
  }

  @Override
  protected void assertConfiguredAsExpected(RestartableJenkinsRule restartableJenkinsRule, String s) {
    SonarInstallation installation = getSonarInstallation();
    assertThat(installation).isNotNull();
    assertThat(installation.getName()).isEqualTo("TEST");
    assertThat(installation.getServerUrl()).isEqualTo("http://url:9000");
  }

  SonarInstallation getSonarInstallation() {
    SonarGlobalConfiguration sonarGlobalConfiguration = GlobalConfiguration.all()
        .get(SonarGlobalConfiguration.class);
    assertThat(sonarGlobalConfiguration).isNotNull();

    SonarInstallation[] installations = sonarGlobalConfiguration.getInstallations();
    assertThat(installations).isNotEmpty();

    return installations[0];
  }

  @Override
  protected String stringInLogExpected() {
    return "credentialsId = test-id";
  }
}
