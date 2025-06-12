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
package hudson.plugins.sonar.utils;

import hudson.util.VersionNumber;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

class JenkinsRouterAfterV2Test {

  @Test
  void testGetGlobalToolConfigUrlAfterV2() {
    try (MockedStatic<Jenkins> jenkinsMock = mockStatic(Jenkins.class)) {

      Jenkins jenkins = Mockito.mock(Jenkins.class);
      jenkinsMock.when(Jenkins::get).thenReturn(jenkins);
      Mockito.when(jenkins.getRootUrl()).thenReturn("http://localhost:8080/");
      jenkinsMock.when(Jenkins::getVersion).thenReturn(new VersionNumber("2"));

      String expectedUrl = "http://localhost:8080/configureTools";
      assertEquals(expectedUrl, JenkinsRouter.getGlobalToolConfigUrl());
    }
  }
}
