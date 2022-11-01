/*
 * SonarQube Scanner for Jenkins
 * Copyright (C) 2007-2022 SonarSource SA
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
package hudson.plugins.sonar.client;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OkHttpClientSingletonTest {

  @Test
  public void should_be_same_instance_during_different_calls() {
    assertThat(OkHttpClientSingleton.getInstance()).isSameAs(OkHttpClientSingleton.getInstance());
  }

}
