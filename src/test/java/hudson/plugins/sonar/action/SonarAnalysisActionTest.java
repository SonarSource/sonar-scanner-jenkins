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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SonarAnalysisActionTest {

  @Test
  void testRoundTrips() {
    SonarAnalysisAction analysis = new SonarAnalysisAction("inst", "credId", "instUrl");
    analysis.setUrl("url1");
    analysis.setNew(false);
    analysis.setCeTaskId("task1");
    analysis.setSkipped(false);

    assertThat(analysis.getCeTaskId()).isEqualTo("task1");
    assertThat(analysis.getUrl()).isEqualTo("url1");
    assertThat(analysis.getInstallationName()).isEqualTo("inst");
    assertThat(analysis.getInstallationUrl()).isEqualTo("instUrl");
    assertThat(analysis.getCredentialsId()).isEqualTo("credId");
    assertThat(analysis.isNew()).isFalse();
    assertThat(analysis.isSkipped()).isFalse();
  }

  @Test
  void testRoundTrips_NoInstUrl() {
    SonarAnalysisAction analysis = new SonarAnalysisAction("inst", "credId", null);
    analysis.setServerUrl("serverUrl");
    analysis.setUrl("url1");
    analysis.setNew(false);
    analysis.setCeTaskId("task1");
    analysis.setSkipped(false);

    assertThat(analysis.getCeTaskId()).isEqualTo("task1");
    assertThat(analysis.getUrl()).isEqualTo("url1");
    assertThat(analysis.getServerUrl()).isEqualTo("serverUrl");
    assertThat(analysis.getInstallationName()).isEqualTo("inst");
    assertThat(analysis.getInstallationUrl()).isEqualTo("serverUrl");
    assertThat(analysis.getCredentialsId()).isEqualTo("credId");
    assertThat(analysis.isNew()).isFalse();
    assertThat(analysis.isSkipped()).isFalse();
  }

  @Test
  void testCopyConstructor() {
    SonarAnalysisAction analysis = new SonarAnalysisAction("inst", "credId", "instUrl");
    analysis.setUrl("url1");
    analysis.setNew(true);
    analysis.setSkipped(true);
    analysis.setCeTaskId("task1");

    SonarAnalysisAction analysis2 = new SonarAnalysisAction(analysis);
    assertThat(analysis2.getUrl()).isEqualTo("url1");
    assertThat(analysis2.getInstallationName()).isEqualTo("inst");
    assertThat(analysis.getInstallationUrl()).isEqualTo("instUrl");
    assertThat(analysis2.getCredentialsId()).isEqualTo("credId");

    // don't copy these
    assertThat(analysis2.getCeTaskId()).isNull();
    assertThat(analysis2.isNew()).isFalse();
    assertThat(analysis2.isSkipped()).isFalse();
  }
}
