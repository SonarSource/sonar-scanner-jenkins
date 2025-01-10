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
package hudson.plugins.sonar.casc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;

import hudson.plugins.sonar.MsBuildSQRunnerInstallation;
import hudson.plugins.sonar.MsBuildSonarQubeRunnerInstaller;
import hudson.plugins.sonar.SonarRunnerInstallation;
import hudson.plugins.sonar.SonarRunnerInstaller;
import hudson.tools.InstallSourceProperty;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolProperty;
import hudson.tools.ToolPropertyDescriptor;
import hudson.util.DescribableList;
import io.jenkins.plugins.casc.misc.RoundTripAbstractTest;
import jenkins.model.Jenkins;
import org.jvnet.hudson.test.RestartableJenkinsRule;

public class CasCToolTest extends RoundTripAbstractTest {

  @Override
  protected String configResource() {
    return "config-tools.yaml";
  }

  @Override
  protected void assertConfiguredAsExpected(RestartableJenkinsRule restartableJenkinsRule, String s) {
    checkMSBuildSQInstallations(restartableJenkinsRule.j.jenkins);
    checkSQInstallations(restartableJenkinsRule.j.jenkins);
  }

  private void checkSQInstallations(Jenkins j) {
      final ToolDescriptor<SonarRunnerInstallation> descriptor = (ToolDescriptor) j.getDescriptor(SonarRunnerInstallation.class);
      final ToolInstallation[] installations = descriptor.getInstallations();
      assertThat(installations, arrayWithSize(2));

      ToolInstallation withInstaller = installations[0];
      assertEquals("SonarQube Scanner", withInstaller.getName());

      final DescribableList<ToolProperty<?>, ToolPropertyDescriptor> properties = withInstaller.getProperties();
      assertThat(properties, hasSize(1));
      final ToolProperty<?> property = properties.get(0);

      assertThat(((InstallSourceProperty)property).installers,
        containsInAnyOrder(
          allOf(instanceOf(SonarRunnerInstaller.class))
        ));

      ToolInstallation withoutInstaller = installations[1];
      assertThat(withoutInstaller,
        allOf(
          hasProperty("name", equalTo("local SonarQube Scanner")),
          hasProperty("home", equalTo("/home/jenkins/agent/tools/sonarscanner")
        )));
  }

  private void checkMSBuildSQInstallations(Jenkins j) {
    final ToolDescriptor<MsBuildSQRunnerInstallation> descriptor = (ToolDescriptor) j.getDescriptor(MsBuildSQRunnerInstallation.class);
    final ToolInstallation[] installations = descriptor.getInstallations();
    assertThat(installations, arrayWithSize(2));

    ToolInstallation withInstaller = installations[0];
    assertEquals("SonarQube MSScanner", withInstaller.getName());

    final DescribableList<ToolProperty<?>, ToolPropertyDescriptor> properties = withInstaller.getProperties();
    assertThat(properties, hasSize(1));
    final ToolProperty<?> property = properties.get(0);

    assertThat(((InstallSourceProperty)property).installers,
      containsInAnyOrder(
        allOf(instanceOf(MsBuildSonarQubeRunnerInstaller.class))
      ));

    ToolInstallation withoutInstaller = installations[1];
    assertThat(withoutInstaller,
      allOf(
        hasProperty("name", equalTo("local SonarQube MSScanner")),
        hasProperty("home", equalTo("/home/jenkins/agent/tools/msscanner")
      )));
  }

  @Override
  protected String stringInLogExpected() {
    return "MsBuildSQRunnerInstallation";
  }
}
