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

import hudson.model.AbstractProject;
import hudson.tasks.Maven;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@WithJenkins
class SonarPublisherDescriptorImplTest {

  private JenkinsRule jenkinsRule;

  @BeforeEach
  void setUp(JenkinsRule rule) {
    jenkinsRule = rule;
  }

  @Test
  void testGetDisplayName() {
    SonarPublisher.DescriptorImpl descriptor = new SonarPublisher.DescriptorImpl();
    assertThat(descriptor.getDisplayName()).isEqualTo(Messages.SonarPublisher_DisplayName());
  }

  @Test
  void testGetDeprecatedInstallations() {
    SonarPublisher.DescriptorImpl descriptor = new SonarPublisher.DescriptorImpl();
    assertThat(descriptor.getDeprecatedInstallations()).isEmpty();
  }

  @Test
  void testIsDeprecatedBuildWrapperEnabled() {
    SonarPublisher.DescriptorImpl descriptor = new SonarPublisher.DescriptorImpl();
    assertThat(descriptor.isDeprecatedBuildWrapperEnabled()).isFalse();
  }

  @Test
  void testGetInstallations() {
    SonarPublisher.DescriptorImpl descriptor = new SonarPublisher.DescriptorImpl();
    assertThat(descriptor.getInstallations()).isNotNull();
  }

  @Test
  void testGetHelpFile() {
    SonarPublisher.DescriptorImpl descriptor = new SonarPublisher.DescriptorImpl();
    assertThat(descriptor.getHelpFile()).isEqualTo("/plugin/sonar/help-sonar-publisher.html");
  }

  @Test
  void testGetHelpFileForField() {
    SonarPublisher.DescriptorImpl descriptor = new SonarPublisher.DescriptorImpl();
    try (MockedStatic<Jenkins> jenkinsMock = mockStatic(Jenkins.class)) {
      Jenkins jenkins = mock(Jenkins.class);
      Maven.DescriptorImpl mavenDescriptor = mock(Maven.DescriptorImpl.class);
      jenkinsMock.when(Jenkins::get).thenReturn(jenkins);
      when(jenkins.getDescriptorByType(Maven.DescriptorImpl.class)).thenReturn(mavenDescriptor);
      when(mavenDescriptor.getHelpFile("settings")).thenReturn("/plugin/maven/help-settings.html");

      assertThat(descriptor.getHelpFile("globalSettings")).isEqualTo("/plugin/maven/help-settings.html");
      assertThat(descriptor.getHelpFile("settings")).isEqualTo("/plugin/maven/help-settings.html");
    }
  }

  @Test
  void testDeleteGlobalConfiguration() {
    SonarPublisher.DescriptorImpl descriptor = new SonarPublisher.DescriptorImpl();
    descriptor.deleteGlobalConfiguration();
    assertThat(descriptor.getDeprecatedInstallations()).isNull();
    assertThat(descriptor.isDeprecatedBuildWrapperEnabled()).isFalse();
  }

  @Test
  void testGetMavenInstallations() {
    SonarPublisher.DescriptorImpl descriptor = new SonarPublisher.DescriptorImpl();
    try (MockedStatic<Jenkins> jenkinsMock = mockStatic(Jenkins.class)) {
      Jenkins jenkins = mock(Jenkins.class);
      Maven.DescriptorImpl mavenDescriptor = mock(Maven.DescriptorImpl.class);
      when(mavenDescriptor.getInstallations()).thenReturn(new Maven.MavenInstallation[0]);
      jenkinsMock.when(Jenkins::get).thenReturn(jenkins);
      when(jenkins.getDescriptorByType(Maven.DescriptorImpl.class)).thenReturn(mavenDescriptor);

      assertThat(descriptor.getMavenInstallations()).isNotNull();
    }
  }

  @Test
  void testIsApplicable() {
    SonarPublisher.DescriptorImpl descriptor = new SonarPublisher.DescriptorImpl();
    assertThat(descriptor.isApplicable(AbstractProject.class)).isTrue();
  }
}
