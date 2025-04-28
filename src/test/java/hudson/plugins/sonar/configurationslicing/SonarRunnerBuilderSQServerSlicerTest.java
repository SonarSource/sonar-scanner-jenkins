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

import hudson.model.FreeStyleProject;
import hudson.plugins.sonar.SonarRunnerBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@WithJenkins
class SonarRunnerBuilderSQServerSlicerTest {

  private JenkinsRule j;

  @BeforeEach
  void setUp(JenkinsRule rule) {
    j = rule;
  }

  @Test
  void availableProjectsWithSonarBuildStep() throws IOException {
    final FreeStyleProject project = j.createFreeStyleProject();
    assertThat(new SonarRunnerBuilderSQServerSlicer().getWorkDomain().size()).isZero();
    project.getBuildersList().add(new SonarRunnerBuilder());
    assertThat(new SonarRunnerBuilderSQServerSlicer().getWorkDomain().size()).isEqualTo(1);
  }

  @Test
  void changeJobAdditionalProperties() throws Exception {
    final FreeStyleProject project = j.createFreeStyleProject();
    final SonarRunnerBuilder mySonar = new SonarRunnerBuilder();
    mySonar.setInstallationName("MySonar");
    project.getBuildersList().add(mySonar);

    final SonarRunnerBuilderSQServerSlicer.SonarRunnerBuilderSQServerSlicerSpec spec = new SonarRunnerBuilderSQServerSlicer.SonarRunnerBuilderSQServerSlicerSpec();
    final List<String> values = spec.getValues(project);
    assertThat(values.get(0)).isEqualTo("MySonar");
    final List<String> newValues = new ArrayList<>();
    newValues.add("MySonar 2");
    spec.setValues(project, newValues);

    assertThat(mySonar.getInstallationName()).isEqualTo("MySonar 2");
  }

}
