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
package hudson.plugins.sonar.action;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SonarProjectActionFactoryTest {
  private SonarProjectActionFactory factory;
  private AbstractProject project;

  @BeforeEach
  void setUp() {
    factory = new SonarProjectActionFactory();
    project = mock(AbstractProject.class);
  }

  @Test
  void testNoBuildInfo() {
    mockProject(true);
    Collection<? extends Action> actions = factory.createFor(project);
    List<SonarProjectIconAction> projectActions = getSonarProjectIconAction(actions);
    assertThat(projectActions).hasSize(1);
    assertThat(projectActions.get(0).getUrlName()).isNull();
    assertThat(projectActions.get(0).getDisplayName()).isNotNull();
    assertThat(projectActions.get(0).getBuildInfo()).isNull();
  }

  @Test
  void testNoRepeatedURLs() {
    SonarAnalysisAction info1 = createBuildInfo("url1");
    SonarAnalysisAction info2 = createBuildInfo("url1");
    mockProject(true, info1, info2);

    when(project.getLastBuild()).thenReturn(null);
    Collection<? extends Action> actions = factory.createFor(project);
    List<SonarProjectIconAction> projectActions = getSonarProjectIconAction(actions);
    assertThat(projectActions).hasSize(1);
    assertThat(projectActions.get(0).getBuildInfo()).isEqualTo(info1);
    assertThat(projectActions.get(0).getUrlName()).isEqualTo("url1");
  }

  @Test
  void testNoLastBuild() {
    mockProject(true);
    when(project.getLastBuild()).thenReturn(null);
    Collection<? extends Action> actions = factory.createFor(project);
    List<SonarProjectIconAction> projectActions = getSonarProjectIconAction(actions);
    assertThat(projectActions).hasSize(1);
    assertThat(projectActions.get(0).getUrlName()).isNull();
    assertThat(projectActions.get(0).getDisplayName()).isNotNull();
    assertThat(projectActions.get(0).getBuildInfo()).isNull();
  }

  @Test
  void testSeveralInfos() {
    SonarAnalysisAction info1 = createBuildInfo("url1");
    SonarAnalysisAction info2 = createBuildInfo("url2");
    SonarAnalysisAction info3 = createBuildInfo("url3");

    mockProject(true, info1, info2, info3);
    Collection<? extends Action> actions = factory.createFor(project);
    assertThat(actions).hasSize(3);
    // should have no project page because resolver returns nothing
    List<SonarProjectIconAction> projectActions = getSonarProjectIconAction(actions);
    assertThat(projectActions).hasSize(3);

    assertThat(projectActions.get(0).getBuildInfo()).isEqualTo(info1);
    assertThat(projectActions.get(0).getUrlName()).isEqualTo("url1");
    assertThat(projectActions.get(1).getBuildInfo()).isEqualTo(info2);
    assertThat(projectActions.get(1).getUrlName()).isEqualTo("url2");
    assertThat(projectActions.get(2).getBuildInfo()).isEqualTo(info3);
    assertThat(projectActions.get(2).getUrlName()).isEqualTo("url3");
  }

  @Test
  void testNoMarker() {
    mockProject(false, createBuildInfo("url"));
    Collection<? extends Action> actions = factory.createFor(project);
    assertThat(actions).isEmpty();
  }

  private static SonarAnalysisAction createBuildInfo(String url) {
    SonarAnalysisAction buildInfo = new SonarAnalysisAction("inst", "credId", null);
    buildInfo.setUrl(url);
    return buildInfo;
  }

  private static List<SonarProjectIconAction> getSonarProjectIconAction(Collection<? extends Action> actions) {
    List<SonarProjectIconAction> list = new LinkedList<>();

    for (Action a : actions) {
      assertThat(a).isInstanceOfAny(SonarProjectIconAction.class, SonarProjectPageAction.class);
      if (a instanceof SonarProjectIconAction) {
        list.add((SonarProjectIconAction) a);
      }
    }

    return list;
  }

  private void mockProject(boolean markProject, SonarAnalysisAction... buildInfos) {
    AbstractBuild build = mock(AbstractBuild.class);
    when(project.getLastCompletedBuild()).thenReturn(build);

    if (markProject) {
      when(build.getActions()).thenReturn(Arrays.asList(buildInfos));
      when(project.getActions()).thenReturn(Collections.singletonList(new SonarMarkerAction()));
    }
  }
}
