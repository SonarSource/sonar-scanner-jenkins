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

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.plugins.sonar.SonarBuildWrapper.DescriptorImpl;
import hudson.plugins.sonar.action.SonarAnalysisAction;
import hudson.plugins.sonar.client.HttpClient;
import hudson.plugins.sonar.client.WsClient;
import hudson.plugins.sonar.model.TriggersConfig;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WithJenkins
class SonarBuildWrapperTest extends SonarTestCase {

  private static final String MYTOKEN = "mytoken";
  private static final String CREDENTIALSID = "mycredentialsid";
  private SonarBuildWrapper wrapper;
  private SonarInstallation installation;
  private PrintStream stream;

  @Override
  @BeforeEach
  protected void setUp(JenkinsRule rule) throws Exception {
    super.setUp(rule);
    installation = spy(createTestInstallation());
    wrapper = new SonarBuildWrapper("local");
    stream = mock(PrintStream.class);
  }

  @Test
  void testInstance() {
    assertThat(wrapper.getInstallationName()).isEqualTo("local");
    assertThat(wrapper.getDescriptor()).isNotNull();
  }

  @Test
  void testInstallationNameRoundTrip() {
    wrapper.setInstallationName("name");
    assertThat(wrapper.getInstallationName()).isEqualTo("name");
  }

  @Test
  void testDescriptor() {
    DescriptorImpl desc = (DescriptorImpl) wrapper.getDescriptor();

    assertThat(desc.getDisplayName()).isEqualTo("Prepare SonarQube Scanner environment");
    assertThat(desc.getHelpFile()).isEqualTo("/plugin/sonar/help-buildWrapper.html");

    assertThat(desc.getSonarInstallations()).isEmpty();
  }

  @Test
  @Disabled("Needs Fix")
  void testLogging() throws Exception {
    // no instance activated -> don't activate masking
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    OutputStream os = wrapper.decorateLogger(mock(AbstractBuild.class), bos);
    IOUtils.write("test password\ntest sonar\n", os, StandardCharsets.UTF_8);
    assertThat(bos).hasToString("test password\ntest sonar\n");

    // with a SQ instance configured (should mask passwords)
    configureSonar(createTestInstallation());
    bos = new ByteArrayOutputStream();
    final AbstractBuild mock = mock(AbstractBuild.class);
    when(mock.getCharset()).thenReturn(StandardCharsets.UTF_8);
    os = wrapper.decorateLogger(mock, bos);
    IOUtils.write("test password\n", os, StandardCharsets.UTF_8);
    assertThat(bos).hasToString("test ******\n");
  }

  @Test
  void maskAuthToken() throws Exception {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    SonarInstallation inst = spy(new SonarInstallation("local", "http://localhost:9001", CREDENTIALSID, null, null, null, null,
            null, new TriggersConfig()));
    addCredential(CREDENTIALSID, MYTOKEN);
    configureSonar(inst);

    AbstractBuild<?, ?> mock = mock(AbstractBuild.class);
    when(mock.getCharset()).thenCallRealMethod();

    OutputStream os = wrapper.decorateLogger(mock, bos);
    IOUtils.write("test sonar\ntest mytoken\n", os, StandardCharsets.UTF_8);
    assertThat(bos).hasToString("test sonar\ntest ******\n");
  }

  @Test
  void testEnvironment() {
    installation = spy(createTestInstallationForEnv());

    EnvVars initialEnvironment = new EnvVars();
    initialEnvironment.put("MY_SERVER", "myserver");
    initialEnvironment.put("MY_PORT", "10000");
    initialEnvironment.put("MY_VALUE", "myValue");
    Map<String, String> map = SonarBuildWrapper.createVars(installation, null, initialEnvironment, mock(Run.class), mockServer(installation));

    assertThat(map).containsEntry("SONAR_HOST_URL", "http://myserver:10000");
    assertThat(map).containsEntry("SONAR_CONFIG_NAME", "local");
    // variable in the value should be resolved
    assertThat(map).containsEntry("SONAR_AUTH_TOKEN", MYTOKEN);
    assertThat(map).containsEntry("SONAR_MAVEN_GOAL", "sonar:sonar");
    assertThat(map).containsEntry("SONAR_EXTRA_PROPS", "-Dkey=myValue -X");

    assertThat(map).containsEntry("SONARQUBE_SCANNER_PARAMS",
            "{ \"sonar.host.url\" : \"http:\\/\\/myserver:10000\", \"sonar.login\" : \"" + MYTOKEN + "\", \"key\" : \"myValue\"}");
  }

  @Test
  void testEnvironmentMojoVersion() {
    installation = new SonarInstallation(null, null, null, null, null, "2.0", null, null, null);

    Map<String, String> map = SonarBuildWrapper.createVars(installation, null, new EnvVars(), mock(Run.class), mock(HttpClient.class));

    assertThat(map).containsEntry("SONAR_MAVEN_GOAL", "org.codehaus.mojo:sonar-maven-plugin:2.0:sonar");
  }

  @Test
  void failOnInvalidInstallationEnvironment() throws Exception {
    // non existing installation
    BuildListener listener = mock(BuildListener.class);
    when(listener.getLogger()).thenReturn(stream);

    try {
      wrapper.setUp(mock(AbstractBuild.class), mock(Launcher.class), listener);
      fail("Expected exception");
    } catch (AbortException e) {
      assertThat(e).hasMessageContaining("does not match");
    }
  }

  @Test
  void decorateWithoutInstallation() throws Exception {
    // no installation
    AbstractBuild<?, ?> build = mock(AbstractBuild.class);
    OutputStream out = wrapper.decorateLogger(build, null);
    assertThat(out).isNull();

    verifyNoInteractions(build);
  }

  @Test
  void testDisableBuildWrapper() {
    configureSonar(createTestInstallation());
    enableBuildWrapper(false);

    assertThat(((DescriptorImpl) wrapper.getDescriptor()).isApplicable(mock(AbstractProject.class))).isFalse();

    enableBuildWrapper(true);

    assertThat(((DescriptorImpl) wrapper.getDescriptor()).isApplicable(mock(AbstractProject.class))).isTrue();
  }

  @Test
  void testBuild() throws Exception {
    // set up a free style project with our wrapper that will execute CaptureVarsBuilder
    configureSonar(installation);
    CaptureVarsBuilder b = new CaptureVarsBuilder();
    FreeStyleProject project = setupFreeStyleProject(b);
    project.getBuildWrappersList().add(wrapper);
    Run<?, ?> build = build(project);

    assertThat(build.getResult()).isEqualTo(Result.SUCCESS);

    // ensure that vars were injected to the job
    assertThat(b.vars).containsEntry("SONAR_HOST_URL", "http://localhost:9001");
    assertThat(b.vars).containsEntry("SONAR_AUTH_TOKEN", MYTOKEN);
    assertThat(b.vars).containsEntry("SONAR_EXTRA_PROPS", "-Dkey=value -X");

    // job's log should have passwords masked
    assertThat(build.getLog(1000)).contains("the token is: ******");
    assertThat(build.getActions(SonarAnalysisAction.class)).hasSize(1);
  }

  @Test
  void testBuild_EnvOnly() throws Exception {
    // set up a free style project with our wrapper that will execute CaptureVarsBuilder
    configureSonar(installation);
    CaptureVarsBuilder b = new CaptureVarsBuilder();
    FreeStyleProject project = setupFreeStyleProject(b);
    wrapper.setEnvOnly(true);
    project.getBuildWrappersList().add(wrapper);
    Run<?, ?> build = build(project);

    assertThat(build.getResult()).isEqualTo(Result.SUCCESS);

    // ensure that vars were injected to the job
    assertThat(b.vars)
            .containsEntry("SONAR_HOST_URL", "http://localhost:9001")
            .containsEntry("SONAR_AUTH_TOKEN", MYTOKEN)
            .containsEntry("SONAR_EXTRA_PROPS", "-Dkey=value -X");

    // job's log should have passwords masked
    assertThat(build.getLog(1000)).contains("the token is: ******");
    assertThat(build.getActions(SonarAnalysisAction.class)).isEmpty();
  }

  @Test
  void testFailedBuild() throws Exception {
    // set up a free style project with our wrapper that will execute CaptureVarsBuilder
    configureSonar(installation);
    CaptureVarsBuilder b = new CaptureVarsBuilder(true);
    FreeStyleProject project = setupFreeStyleProject(b);
    project.getBuildWrappersList().add(wrapper);
    Run<?, ?> build = build(project);

    assertThat(build.getResult()).isEqualTo(Result.FAILURE);

    assertThat(build.getLog(1000)).contains("the token is: ******");
    assertThat(build.getActions(SonarAnalysisAction.class)).hasSize(1);
  }

  private static HttpClient mockServer(SonarInstallation installation) {
    HttpClient client = mock(HttpClient.class);
    when(client.getHttp(installation.getServerUrl() + WsClient.API_VERSION, null)).thenReturn("9.9");
    return client;
  }

  private static class CaptureVarsBuilder extends TestBuilder {
    private final boolean fail;
    private Map<String, String> vars;

    CaptureVarsBuilder() {
      this.fail = false;
    }

    CaptureVarsBuilder(boolean fail) {
      this.fail = fail;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
      EnvVars env = build.getEnvironment(listener);
      vars = new HashMap<>(env);
      listener.getLogger().println("the token is: " + MYTOKEN);
      if (fail) {
        throw new AbortException("asd");
      }
      return true;
    }
  }

  private void enableBuildWrapper(boolean enable) {
    SonarGlobalConfiguration.get().setBuildWrapperEnabled(enable);
  }

  private SonarInstallation createTestInstallation() {
    addCredential(CREDENTIALSID, MYTOKEN);

    return new SonarInstallation("local", "http://localhost:9001", CREDENTIALSID, null, null, null,
            "-X", "key=value", new TriggersConfig());
  }

  private SonarInstallation createTestInstallationForEnv() {
    addCredential(CREDENTIALSID, MYTOKEN);

    return new SonarInstallation("local", "http://$MY_SERVER:$MY_PORT", CREDENTIALSID, null, null, null,
            "-X", "key=$MY_VALUE", new TriggersConfig());
  }

}
