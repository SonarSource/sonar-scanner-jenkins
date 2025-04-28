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
package hudson.plugins.sonar.configurationslicing;

import hudson.maven.MavenModuleSet;
import hudson.plugins.sonar.SonarPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author drautureau
 */
@WithJenkins
class SonarPublisherAdditionalPropertiesSlicerTest {

  private JenkinsRule j;

  @BeforeEach
  void setUp(JenkinsRule rule) {
    j = rule;
  }

  @Test
  void availableMavenProjectsWithSonarPublisher() throws Exception {
    final MavenModuleSet project = j.jenkins.createProject(MavenModuleSet.class, "random-name");
    assertThat(new SonarPublisherAdditionalPropertiesSlicer().getWorkDomain().size()).isZero();
    project.getPublishersList().add(new SonarPublisher("MySonar", null, null, "-Dsonar.verbose", null, null, null, null, null, null, false));
    assertThat(new SonarPublisherAdditionalPropertiesSlicer().getWorkDomain().size()).isEqualTo(1);
  }

  @Test
  void changeJobAdditionalProperties() throws Exception {
    final MavenModuleSet project = j.jenkins.createProject(MavenModuleSet.class, "random-name");
    project.getPublishersList().add(new SonarPublisher("MySonar", null, null, "-Dsonar.verbose", null, null, null, null, null, null, false));
    final SonarPublisherAdditionalPropertiesSlicer.SonarPublisherAdditionalPropertiesSlicerSpec propertiesSpec = new SonarPublisherAdditionalPropertiesSlicer.SonarPublisherAdditionalPropertiesSlicerSpec();
    final List<String> values = propertiesSpec.getValues(project);
    assertThat(values.get(0)).isEqualTo("-Dsonar.verbose");

    final List<String> newValues = new ArrayList<>();
    newValues.add("-Dsonar.showSql");
    propertiesSpec.setValues(project, newValues);
    final SonarPublisher publisher = project.getPublishersList().get(SonarPublisher.class);
    assertThat(publisher.getJobAdditionalProperties()).isEqualTo("-Dsonar.showSql");
  }

}
