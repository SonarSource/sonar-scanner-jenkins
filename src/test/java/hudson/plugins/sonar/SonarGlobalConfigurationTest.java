/*
 * SonarQube Scanner for Jenkins
 * Copyright (C) 2007-2025 SonarSource Sàrl
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

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.util.FormValidation.Kind;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@WithJenkins
class SonarGlobalConfigurationTest extends SonarTestCase {
  private SonarGlobalConfiguration globalConfiguration;
  private SonarInstallation testInstallation;
  private SonarPublisher.DescriptorImpl publisher;

  @Override
  @BeforeEach
  protected void setUp(JenkinsRule rule) throws Exception {
    super.setUp(rule);
    testInstallation = createInstallation("testInst");
    globalConfiguration = new SonarGlobalConfiguration();
    globalConfiguration.dataMigrated = false;
    publisher = getPublisherDescr(true, testInstallation);
  }

  private static SonarPublisher.DescriptorImpl getPublisherDescr(boolean buildWrapperEnabled, SonarInstallation... installations) {
    SonarPublisher.DescriptorImpl publisher = Jenkins.get().getDescriptorByType(SonarPublisher.DescriptorImpl.class);
    publisher.setDeprecatedInstallations(installations);
    publisher.setDeprecatedBuildWrapperEnabled(buildWrapperEnabled);
    return publisher;
  }

  private static SonarInstallation createInstallation(String name) {
    return new SonarInstallation(name, null, null, null, null, null, null, null, null);
  }

  @Test
  void testMigration() {
    globalConfiguration.migrateData();
    assertThat(globalConfiguration.isBuildWrapperEnabled()).isTrue();
    assertThat(globalConfiguration.getInstallations()).containsOnly(testInstallation);

    assertThat(publisher.getDeprecatedInstallations()).isNull();
  }

  @Test
  void testDontOverwriteInMigration() {
    SonarInstallation existing = createInstallation("my installation");
    globalConfiguration.setInstallations(existing);
    globalConfiguration.setBuildWrapperEnabled(false);

    globalConfiguration.migrateData();

    assertThat(globalConfiguration.isBuildWrapperEnabled()).isFalse();
    assertThat(globalConfiguration.getInstallations()).containsOnly(existing);
    assertThat(publisher.getDeprecatedInstallations()).isNull();
  }

  @Test
  void testNameValidation() {
    assertThat(globalConfiguration.doCheckName("").kind).isEqualTo(Kind.ERROR);
    assertThat(globalConfiguration.doCheckName(null).kind).isEqualTo(Kind.ERROR);
    assertThat(globalConfiguration.doCheckName("   ").kind).isEqualTo(Kind.ERROR);
    assertThat(globalConfiguration.doCheckName("asd").kind).isEqualTo(Kind.OK);
  }

  @Test
  void testDoFillWebhookSecretIdItemsAsNonAdminister() throws Exception {
    mockJenkins(false);
    addCredential("description");
    ListBoxModel boxModel = globalConfiguration.doFillWebhookSecretIdItems("secretId");
    assertThat(boxModel.iterator()).hasSize(1);
    assertThat(boxModel.get(0).value).isEqualTo("secretId");
  }

  @Test
  void testDoFillWebhookSecretIdItemsAsAdminister() throws Exception {
    mockJenkins(true);
    addCredential("description");

    ListBoxModel boxModel = globalConfiguration.doFillWebhookSecretIdItems("secretId");
    List<String> optionNames = boxModel.stream()
            .map(option -> option.name)
            .toList();
    assertThat(optionNames).containsExactlyInAnyOrder("description", "- none -");
  }

  @Test
  void testDoFillCredentialsIdItemsAsNonAdminister() throws Exception {
    addCredential("description");
    mockJenkins(false);
    ListBoxModel boxModel = globalConfiguration.doFillCredentialsIdItems("credentialId");
    assertThat(boxModel.iterator()).hasSize(1);
    assertThat(boxModel.get(0).value).isEqualTo("credentialId");
  }

  @Test
  void testDoFillCredentialsIdItemsAsAdminister() throws Exception {
    mockJenkins(true);
    addCredential("description");

    ListBoxModel boxModel = globalConfiguration.doFillCredentialsIdItems("credentialId");
    List<String> optionNames = boxModel.stream()
            .map(option -> option.name)
            .toList();
    assertThat(optionNames).containsExactlyInAnyOrder("description", "- none -");
  }

  private void mockJenkins(boolean administer) {
    Jenkins jenkins = mock(Jenkins.class);
    when(jenkins.hasPermission(Jenkins.ADMINISTER)).thenReturn(administer);
    globalConfiguration = new SonarGlobalConfiguration(() -> jenkins);
  }

  private void addCredential(String description) throws Exception {
    StringCredentialsImpl c = new StringCredentialsImpl(CredentialsScope.USER, "id", description, Secret.fromString("value"));
    CredentialsProvider.lookupStores(j).iterator().next().addCredentials(Domain.global(), c);
  }

}
