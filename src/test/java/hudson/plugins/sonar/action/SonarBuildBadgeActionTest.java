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

import hudson.plugins.sonar.SonarTestCase;

import hudson.plugins.sonar.action.SonarBuildBadgeAction;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Evgeny Mandrikov
 */
public class SonarBuildBadgeActionTest extends SonarTestCase {
  @Test
  public void test() throws Exception {
    SonarBuildBadgeAction action = new SonarBuildBadgeAction();
    assertThat(action.getIconFileName()).isNull();
    assertThat(action.getUrlName()).isNull();

    assertThat(action.getDisplayName()).isNotNull();
    assertThat(action.getIcon()).isNotNull();
    assertThat(action.getTooltip()).isNotNull();
  }
}
