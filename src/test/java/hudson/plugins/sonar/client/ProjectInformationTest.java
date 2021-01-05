/*
 * SonarQube Scanner for Jenkins
 * Copyright (C) 2007-2021 SonarSource SA
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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class ProjectInformationTest {

  @Test
  public void testRoundTrips() {
    ProjectInformation proj = new ProjectInformation();
    proj.setName("name");
    proj.setStatus("status");
    proj.setUrl("url");
    String[] errors = {"error1", "error2", "error3"};
    proj.setErrors(errors);
    proj.setCeUrl("ceUrl");
    proj.setCeStatus("ceStatus");

    assertThat(proj.getCeStatus()).isEqualTo("cestatus");
    assertThat(proj.getCeUrl()).isEqualTo("ceUrl");
    assertThat(proj.getErrors()).contains(errors);
    assertThat(proj.getUrl()).isEqualTo("url");
    assertThat(proj.getProjectName()).isEqualTo("name");
    assertThat(proj.hasErrors()).isTrue();
    assertThat(proj.getBadgeStatus()).isEqualTo(ProjectInformation.UNKNOWN_MESSAGE);
  }

  @Test
  @UseDataProvider("qualityGateStatuses")
  public void testGetBadgeStatus(String status, String badgeStatus) {
    ProjectInformation info = new ProjectInformation();
    info.setStatus(status);

    assertThat(info.getBadgeStatus()).isEqualTo(badgeStatus);
  }

  @DataProvider
  public static Object[][] qualityGateStatuses() {
    return new Object[][] {
      {"OK", ProjectInformation.OK_MESSAGE},
      {"ok", ProjectInformation.OK_MESSAGE},
      {"WARN", ProjectInformation.WARN_MESSAGE},
      {"warn", ProjectInformation.WARN_MESSAGE},
      {"ERROR", ProjectInformation.ERROR_MESSAGE},
      {"error", ProjectInformation.ERROR_MESSAGE},
      {"Something Else", ProjectInformation.UNKNOWN_MESSAGE},
      {null, ProjectInformation.UNKNOWN_MESSAGE},
    };
  }
}
