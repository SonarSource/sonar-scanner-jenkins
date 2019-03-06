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
package hudson.plugins.sonar.utils;

import com.google.common.collect.ImmutableMap;
import hudson.EnvVars;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.entry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

public class BuilderUtilsTest {
  @Test
  public void buildEnv() throws IOException, InterruptedException {
    TaskListener l = mock(TaskListener.class);
    AbstractBuild<?, ?> r = mock(AbstractBuild.class);
    EnvVars env = new EnvVars("key", "value", "key2", "value2");

    when(r.getEnvironment(l)).thenReturn(env);
    when(r.getBuildVariables()).thenReturn(ImmutableMap.of("key", "newValue"));

    env = BuilderUtils.getEnvAndBuildVars(r, l);

    assertThat(env.descendingMap()).contains(entry("key", "newValue"), entry("key2", "value2"));
  }
}
