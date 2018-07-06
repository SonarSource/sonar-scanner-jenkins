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

import hudson.EnvVars;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.plugins.sonar.AbstractMsBuildSQRunner.SonarQubeScannerMsBuildParams;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import javax.annotation.Nullable;

import hudson.util.Secret;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class MsBuildSQRunnerBeginTest extends MsBuildSQRunnerTest {

  @Test
  public void testNormalExec() throws Exception {
    configureDefaultSonar();
    configureMsBuildScanner(false);

    FreeStyleProject proj = createFreeStyleProjectWithMSBuild("default", "default");
    Run<?, ?> r = build(proj, Result.SUCCESS);

    assertLogContains(getTestExeName() + " begin /k:key /n:name /v:1.0", r);
    assertLogContains("This is a fake MS Build Scanner", r);
    assertThat(r.getAction(SonarQubeScannerMsBuildParams.class)).isNotNull();
  }

  @Test
  public void testNormalExecWithEnvVar() throws Exception {
    configureDefaultSonar();
    configureMsBuildScanner(false);
    addEnvVar("CUSTOM_KEY", "customKey");

    FreeStyleProject proj = createFreeStyleProjectWithMSBuild("default", "default", "$CUSTOM_KEY", null);
    Run<?, ?> r = build(proj, Result.SUCCESS);

    assertLogContains(getTestExeName() + " begin /k:customKey /n:name /v:1.0", r);
    assertLogContains("This is a fake MS Build Scanner", r);
    assertThat(r.getAction(SonarQubeScannerMsBuildParams.class)).isNotNull();
  }

  @Test
  public void failExe() throws Exception {
    configureDefaultSonar();
    configureMsBuildScanner(true);

    FreeStyleProject proj = createFreeStyleProjectWithMSBuild("default", "default");
    Run<?, ?> r = build(proj, Result.FAILURE);

    assertLogContains(getTestExeName() + " begin /k:key /n:name /v:1.0", r);
    assertLogContains("This is a fake MS Build Scanner", r);
  }

  @Test
  public void additionalArgs() throws Exception {
    SonarInstallation inst = new SonarInstallation("default", null,
      null, null, "/x:a=b", null, "key=value");
    configureSonar(inst);
    configureMsBuildScanner(true);

    FreeStyleProject proj = createFreeStyleProjectWithMSBuild("default", "default", "key", "/y");
    Run<?, ?> r = build(proj, Result.FAILURE);

    assertLogContains(getTestExeName() + " begin /k:key /n:name /v:1.0 /d:key=value /x:a=b /y", r);
    assertLogContains("This is a fake MS Build Scanner", r);
  }

  @Test
  public void testSonarProps() throws Exception {
    SonarInstallation inst = spy(new SonarInstallation("default", "http://dummy-server:9090", "credentialsId",
      null, null, null, null));
    when(inst.getServerAuthenticationToken(any(Run.class))).thenReturn("token");
    configureSonar(inst);
    configureMsBuildScanner(false);

    FreeStyleProject proj = createFreeStyleProjectWithMSBuild("default", "default");
    Run<?, ?> r = build(proj, Result.SUCCESS);
    assertLogContains(getTestExeName() + " begin /k:key /n:name /v:1.0"
      + " /d:sonar.host.url=http://dummy-server:9090 ********", r);
    assertLogContains("This is a fake MS Build Scanner", r);
    assertLogDoesntContains("mypass", r);
  }

  @Test
  public void testNoMsBuildInst() throws Exception {
    configureDefaultSonar();
    configureMsBuildScanner(false);

    FreeStyleProject proj = createFreeStyleProjectWithMSBuild("default", "non-existing");
    Run<?, ?> r = build(proj, Result.FAILURE);
    assertLogContains("No SonarScanner for MSBuild installation found", r);
  }

  @Test
  public void testDotNetCoreScanner() throws Exception {
    configureDefaultSonar();
    configureMsBuildScanner(false);

    FreeStyleProject proj = createFreeStyleProjectWithMSBuild("default", "default");
    MsBuildSQRunnerInstallation.setTestExeName("Foo.dll");
    Run<?, ?> r = build(proj, Result.FAILURE);

    String log = JenkinsRule.getLog(r);
    assertThat(log).containsPattern("dotnet .*Foo.dll begin /k:key /n:name /v:1.0");
  }

  private void addEnvVar(String key, String value) {
    EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
    EnvVars envVars = prop.getEnvVars();
    envVars.put(key, value);
    j.jenkins.getGlobalNodeProperties().add(prop);
  }

  private FreeStyleProject createFreeStyleProjectWithMSBuild(String sonarInst, String msBuildInst) throws Exception {
    return createFreeStyleProjectWithMSBuild(sonarInst, msBuildInst, "key", null);
  }

  private FreeStyleProject createFreeStyleProjectWithMSBuild(String sonarInst, String msBuildInst, String key, @Nullable String additionalArgs) throws Exception {
    return setupFreeStyleProject(new MsBuildSQRunnerBegin(msBuildInst, sonarInst, key, "name", "1.0", additionalArgs));
  }
}
