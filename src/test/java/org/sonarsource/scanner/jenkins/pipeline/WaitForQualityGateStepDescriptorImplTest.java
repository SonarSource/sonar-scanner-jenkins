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
package org.sonarsource.scanner.jenkins.pipeline;

import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.TopLevelItem;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.util.UUID;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class WaitForQualityGateStepDescriptorImplTest {

  @Rule
  public JenkinsRule jenkinsRule = new JenkinsRule();


  @Test
  public void testDoFillCredentialsIdItems_ProjectNull_NoAdminPermission() {
    try (MockedStatic<Jenkins> jenkinsMock = mockStatic(Jenkins.class)) {
      Jenkins jenkins = mock(Jenkins.class);
      jenkinsMock.when(Jenkins::get).thenReturn(jenkins);
      when(jenkins.hasPermission(Jenkins.ADMINISTER)).thenReturn(false);

      ListBoxModel items = WaitForQualityGateStep.DescriptorImpl.doFillCredentialsIdItems(null, "mycredentialsid");
      assertThat(items)
        .extracting(option -> option.value)
        .contains("mycredentialsid");
    }
  }

  @Test
  public void testDoFillCredentialsIdItems_ProjectNull_HasAdminPermission() {
    try (MockedStatic<Jenkins> jenkinsMock = mockStatic(Jenkins.class)) {
      Jenkins jenkins = mock(Jenkins.class);
      jenkinsMock.when(Jenkins::get).thenReturn(jenkins);
      when(jenkins.hasPermission(Jenkins.ADMINISTER)).thenReturn(true);

      ListBoxModel items = WaitForQualityGateStep.DescriptorImpl.doFillCredentialsIdItems(null, "mycredentialsid");
      assertThat(items)
        .extracting(option -> option.value)
        .contains("mycredentialsid");
    }
  }

  @Test
  public void testDoFillCredentialsIdItems_ProjectHasExtendedReadPermission() {
    Item project = mock(Item.class);
    when(project.hasPermission(Item.EXTENDED_READ)).thenReturn(true);

    ListBoxModel items = WaitForQualityGateStep.DescriptorImpl.doFillCredentialsIdItems(project, "mycredentialsid");
    assertThat(items)
      .extracting(option -> option.value)
      .contains("mycredentialsid");
  }

  @Test
  public void testDoFillCredentialsIdItems_ProjectNoExtendedReadPermission() {
    Item project = mock(Item.class);
    when(project.hasPermission(Item.EXTENDED_READ)).thenReturn(false);

    ListBoxModel items = WaitForQualityGateStep.DescriptorImpl.doFillCredentialsIdItems(project, "mycredentialsid");
    assertThat(items)
      .extracting(option -> option.value)
      .contains("mycredentialsid");
  }

  @Test
  public void testDoFillCredentialsIdItems_FakeProject() {
    ItemGroup<TopLevelItem> itemGroup = mock(ItemGroup.class);
    FreeStyleProject project = new FreeStyleProject(itemGroup, "fake-" + UUID.randomUUID());

    ListBoxModel items = WaitForQualityGateStep.DescriptorImpl.doFillCredentialsIdItems(project, "mycredentialsid");
    assertThat(items)
      .extracting(option -> option.value)
      .contains("mycredentialsid");
  }

  @Test
  public void testDoCheckCredentialsId_ProjectNull_NoAdminPermission() {
    try (MockedStatic<Jenkins> jenkinsMock = mockStatic(Jenkins.class)) {
      Jenkins jenkins = mock(Jenkins.class);
      jenkinsMock.when(Jenkins::get).thenReturn(jenkins);
      when(jenkins.hasPermission(Jenkins.ADMINISTER)).thenReturn(false);

      FormValidation validation = WaitForQualityGateStep.DescriptorImpl.doCheckCredentialsId(null, "mycredentialsid");
      assertThat(validation.kind).isEqualTo(FormValidation.Kind.OK);
    }
  }

  @Test
  public void testDoCheckCredentialsId_ProjectNull_HasAdminPermission() {
    try (MockedStatic<Jenkins> jenkinsMock = mockStatic(Jenkins.class)) {
      Jenkins jenkins = mock(Jenkins.class);
      jenkinsMock.when(Jenkins::get).thenReturn(jenkins);
      when(jenkins.hasPermission(Jenkins.ADMINISTER)).thenReturn(true);

      FormValidation validation = WaitForQualityGateStep.DescriptorImpl.doCheckCredentialsId(null, "mycredentialsid");
      assertThat(validation.kind).isEqualTo(FormValidation.Kind.WARNING);
    }
  }

  @Test
  public void testDoCheckCredentialsId_ProjectHasExtendedReadPermission() {
    Item project = mock(Item.class);
    when(project.hasPermission(Item.EXTENDED_READ)).thenReturn(true);

    FormValidation validation = WaitForQualityGateStep.DescriptorImpl.doCheckCredentialsId(project, "mycredentialsid");
    assertThat(validation.kind).isEqualTo(FormValidation.Kind.WARNING);
  }

  @Test
  public void testDoCheckCredentialsId_ProjectNoExtendedReadPermission() {
    Item project = mock(Item.class);
    when(project.hasPermission(Item.EXTENDED_READ)).thenReturn(false);

    FormValidation validation = WaitForQualityGateStep.DescriptorImpl.doCheckCredentialsId(project, "mycredentialsid");
    assertThat(validation.kind).isEqualTo(FormValidation.Kind.OK);
  }

  @Test
  public void testDoCheckCredentialsId_InvalidCredentialsId() {
    Item project = mock(Item.class);
    when(project.hasPermission(Item.EXTENDED_READ)).thenReturn(true);

    FormValidation validation = WaitForQualityGateStep.DescriptorImpl.doCheckCredentialsId(project, "invalidcredentialsid");
    assertThat(validation.kind).isEqualTo(FormValidation.Kind.WARNING);
  }
}
