/*
 * SonarQube Scanner for Jenkins
 * Copyright (C) 2007-2019 SonarSource SA
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

import hudson.model.AbstractProject;
import hudson.plugins.sonar.SonarTestCase;
import hudson.util.RunList;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Evgeny Mandrikov
 */
public class SonarProjectIconActionTest extends SonarTestCase {
  @Test
  public void test() throws Exception {
    AbstractProject project = mock(AbstractProject.class);
    SonarProjectIconAction action = new SonarProjectIconAction(new SonarAnalysisAction("inst", "credId"));
    when(project.getBuilds()).thenReturn(new RunList());
    assertThat(action.getDisplayName()).isNotNull();
    assertThat(action.getIconFileName()).isNotNull();
    assertThat(action.getUrlName()).isNull();
  }
}
