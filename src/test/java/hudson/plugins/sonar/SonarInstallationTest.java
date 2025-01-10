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
package hudson.plugins.sonar;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.Util;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.sonar.model.TriggersConfig;
import hudson.util.Secret;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;

/**
 * @author Evgeny Mandrikov
 */
public class SonarInstallationTest extends SonarTestCase {

  @Test
  public void testRoundtrip() throws IOException, ExecutionException, InterruptedException {
    TriggersConfig triggers = new TriggersConfig();
    SonarGlobalConfiguration d = new SonarGlobalConfiguration();
    String credentialsId = "credentialsId";
    SonarInstallation inst = spy(new SonarInstallation(
      "Name",
      "server.url",
      credentialsId,
      null,
      "secretId",
      "mojoVersion",
      "props",
      "key=value",
      triggers));
    StringCredentials cred = new StringCredentialsImpl(CredentialsScope.GLOBAL, credentialsId, null, Secret.fromString("token"));

    CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
    store.addCredentials(Domain.global(), cred);

    FreeStyleProject freeStyleProject = j.createFreeStyleProject();
    FreeStyleBuild run = new FreeStyleBuild(freeStyleProject);

    d.setInstallations(inst);

    SonarInstallation i = new SonarGlobalConfiguration().getInstallations()[0];
    String storedConfig = Util.loadFile(new File(Jenkins.get().getRootDir(), d.getId() + ".xml"), StandardCharsets.UTF_8);

    assertThat(i.getName()).isEqualTo("Name");
    assertThat(i.getServerUrl()).isEqualTo("server.url");
    assertThat(i.getServerAuthenticationToken(run)).isEqualTo("token");
    assertThat(i.getMojoVersion()).isEqualTo("mojoVersion");
    assertThat(i.getAdditionalProperties()).isEqualTo("props");
    assertThat(i.getAdditionalAnalysisProperties()).isEqualTo("key=value");

    assertThat(storedConfig).doesNotContain("dbPasswd");
    assertThat(storedConfig).doesNotContain("sonarPasswd");
  }

  @Test
  public void testRoundtripWithoutToken() throws IOException {
    TriggersConfig triggers = new TriggersConfig();
    SonarInstallation inst = new SonarInstallation(
      "Name",
      "server.url",
      null,
      "mojoVersion",
      "props",
      triggers,
      "key=value");
    SonarGlobalConfiguration d = new SonarGlobalConfiguration();
    d.setInstallations(inst);
    d.save();

    SonarInstallation i = new SonarGlobalConfiguration().getInstallations()[0];
    String storedConfig = Util.loadFile(new File(Jenkins.get().getRootDir(), d.getId() + ".xml"), StandardCharsets.UTF_8);

    assertThat(i.getName()).isEqualTo("Name");
    assertThat(i.getServerUrl()).isEqualTo("server.url");
    assertThat(i.getServerAuthenticationToken(any())).isEqualTo(null);
    assertThat(i.getMojoVersion()).isEqualTo("mojoVersion");
    assertThat(i.getAdditionalProperties()).isEqualTo("props");
    assertThat(i.getAdditionalAnalysisProperties()).isEqualTo("key=value");

    assertThat(storedConfig).doesNotContain("dbPasswd");
    assertThat(storedConfig).doesNotContain("sonarPasswd");
  }

  @Test
  public void testAnalysisPropertiesWindows() {
    assertAnalysisPropsWindows("key=value", "/d:key=value");
    assertAnalysisPropsWindows("key=value key2=value2", "/d:key=value", "/d:key2=value2");
    assertAnalysisPropsWindows("-Dkey=value", "/d:-Dkey=value");
    assertAnalysisPropsWindows("");
    assertAnalysisPropsWindows(null);
  }

  @Test
  public void testAnalysisPropertiesUnix() {
    assertAnalysisPropsUnix("key=value", "-Dkey=value");
    assertAnalysisPropsUnix("key=value key2=value2", "-Dkey=value", "-Dkey2=value2");
    assertAnalysisPropsUnix("-Dkey=value", "-D-Dkey=value");
    assertAnalysisPropsUnix("");
    assertAnalysisPropsUnix(null);
  }

  private void assertAnalysisPropsWindows(String input, String... expectedEntries) {
    SonarInstallation inst = new SonarInstallation(null, null, null, null, null, null, null, input, null);
    assertThat(inst.getAdditionalAnalysisPropertiesWindows()).isEqualTo(expectedEntries);
  }

  private void assertAnalysisPropsUnix(String input, String... expectedEntries) {
    SonarInstallation inst = new SonarInstallation(null, null, null, null, null, null, null, input, null);
    assertThat(inst.getAdditionalAnalysisPropertiesUnix()).isEqualTo(expectedEntries);
  }
}
