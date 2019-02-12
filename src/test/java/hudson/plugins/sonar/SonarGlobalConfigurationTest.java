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
package hudson.plugins.sonar;

import hudson.util.FormValidation.Kind;
import jenkins.model.Jenkins;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SonarGlobalConfigurationTest extends SonarTestCase {
  private SonarGlobalConfiguration globalConfiguration;
  private SonarInstallation testInstallation;
  private SonarPublisher.DescriptorImpl publisher;

  @Before
  public void setUp() throws ClassNotFoundException {
    testInstallation = createInstallation("testInst");
    globalConfiguration = new SonarGlobalConfiguration();
    globalConfiguration.dataMigrated = false;
    publisher = getPublisherDescr(true, testInstallation);
  }

  private static SonarPublisher.DescriptorImpl getPublisherDescr(boolean buildWrapperEnabled, SonarInstallation... installations) {
    SonarPublisher.DescriptorImpl publisher = Jenkins.getInstance().getDescriptorByType(SonarPublisher.DescriptorImpl.class);
    publisher.setDeprecatedInstallations(installations);
    publisher.setDeprecatedBuildWrapperEnabled(buildWrapperEnabled);
    return publisher;
  }

  private static SonarInstallation createInstallation(String name) {
    return new SonarInstallation(name, null, null,null, null, null, null, null);
  }

  @Test
  public void testMigration() {
    globalConfiguration.migrateData();
    assertThat(globalConfiguration.isBuildWrapperEnabled()).isTrue();
    assertThat(globalConfiguration.getInstallations()).containsOnly(testInstallation);

    assertThat(publisher.getDeprecatedInstallations()).isNull();
  }

  @Test
  public void testDontOverwriteInMigration() {
    SonarInstallation existing = createInstallation("my installation");
    globalConfiguration.setInstallations(existing);
    globalConfiguration.setBuildWrapperEnabled(false);

    globalConfiguration.migrateData();

    assertThat(globalConfiguration.isBuildWrapperEnabled()).isFalse();
    assertThat(globalConfiguration.getInstallations()).containsOnly(existing);
    assertThat(publisher.getDeprecatedInstallations()).isNull();
  }

  @Test
  public void testMandatory() {
    assertThat(globalConfiguration.doCheckMandatory("").kind).isEqualTo(Kind.ERROR);
    assertThat(globalConfiguration.doCheckMandatory(null).kind).isEqualTo(Kind.ERROR);
    assertThat(globalConfiguration.doCheckMandatory("   ").kind).isEqualTo(Kind.ERROR);
    assertThat(globalConfiguration.doCheckMandatory("asd").kind).isEqualTo(Kind.OK);
  }

}
