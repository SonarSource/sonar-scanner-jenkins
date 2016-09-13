/*
 * Jenkins Plugin for SonarQube, open source software quality management tool.
 * mailto:contact AT sonarsource DOT com
 *
 * Jenkins Plugin for SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Jenkins Plugin for SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
/*
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
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
import hudson.model.Run.RunnerAbortedException;
import hudson.plugins.sonar.SonarBuildWrapper.DescriptorImpl;
import hudson.plugins.sonar.action.SonarAnalysisAction;
import hudson.plugins.sonar.model.TriggersConfig;
import hudson.plugins.sonar.utils.SQServerVersions;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.TestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class SonarBuildWrapperTest extends SonarTestCase {

  private SonarBuildWrapper wrapper;
  private SonarInstallation installation;
  private PrintStream stream;

  @Before
  public void setUp() {
    wrapper = new SonarBuildWrapper("local");
    installation = createTestInstallation();
    stream = mock(PrintStream.class);
  }

  @Test
  public void testInstance() {
    assertThat(wrapper.getInstallationName()).isEqualTo("local");
    assertThat(wrapper.getDescriptor()).isNotNull();
  }

  @Test
  public void testInstallationNameRoundTrip() {
    wrapper.setInstallationName("name");
    assertThat(wrapper.getInstallationName()).isEqualTo("name");
  }

  @Test
  public void testDescriptor() {
    DescriptorImpl desc = (DescriptorImpl) wrapper.getDescriptor();

    assertThat(desc.getDisplayName()).isEqualTo("Prepare SonarQube Scanner environment");
    assertThat(desc.getHelpFile()).isEqualTo("/plugin/sonar/help-buildWrapper.html");

    assertThat(desc.getSonarInstallations()).isEmpty();
  }

  public void testLogging() throws RunnerAbortedException, IOException, InterruptedException {
    // no instance activated -> don't activate masking
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    OutputStream os = wrapper.decorateLogger(mock(AbstractBuild.class), bos);
    IOUtils.write("test password\ntest sonar\n", os);
    assertThat(new String(bos.toByteArray())).isEqualTo("test password\ntest sonar\n");

    // with a SQ instance configured (should mask passwords)
    configureSonar(createTestInstallation());
    bos = new ByteArrayOutputStream();
    os = wrapper.decorateLogger(mock(AbstractBuild.class), bos);
    IOUtils.write("test password\n", os);
    assertThat(new String(bos.toByteArray())).isEqualTo("test ******\n");
  }

  @Test
  public void dontMaskDefaultPassword() throws IOException, InterruptedException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    configureSonar(new SonarInstallation("local", "http://localhost:9001", null, null, null, null, null,
      null, null, new TriggersConfig(), "$SONAR_CONFIG_NAME", null, null));

    OutputStream os = wrapper.decorateLogger(mock(AbstractBuild.class), bos);
    IOUtils.write("test sonar\ntest something\n", os);
    assertThat(new String(bos.toByteArray())).isEqualTo("test sonar\ntest something\n");
  }

  @Test
  public void maskAuthToken() throws IOException, InterruptedException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    configureSonar(new SonarInstallation("local", "http://localhost:9001", SQServerVersions.SQ_5_3_OR_HIGHER, "mytoken", null, null, null,
      null, null, new TriggersConfig(), "$SONAR_CONFIG_NAME", null, null));

    OutputStream os = wrapper.decorateLogger(mock(AbstractBuild.class), bos);
    IOUtils.write("test sonar\ntest mytoken\n", os);
    assertThat(new String(bos.toByteArray())).isEqualTo("test sonar\ntest ******\n");
  }

  @Test
  public void testEnvironment52() {
    Map<String, String> map = SonarBuildWrapper.createVars(installation);

    assertThat(map).containsEntry("SONAR_HOST_URL", "http://localhost:9001");
    assertThat(map).containsEntry("SONAR_CONFIG_NAME", "local");
    // variable in the value should be resolved
    assertThat(map).containsEntry("SONAR_AUTH_TOKEN", "");
    assertThat(map).containsEntry("SONAR_LOGIN", "local");
    assertThat(map).containsEntry("SONAR_PASSWORD", "password");
    assertThat(map).containsEntry("SONAR_JDBC_URL", "");
    assertThat(map).containsEntry("SONAR_JDBC_USERNAME", "");
    assertThat(map).containsEntry("SONAR_JDBC_PASSWORD", "");
    assertThat(map).containsEntry("SONAR_MAVEN_GOAL", "sonar:sonar");
    assertThat(map).containsEntry("SONAR_EXTRA_PROPS", "-Dkey=value -X");
  }

  @Test
  public void testEnvironment53() {
    installation = createTestInstallation(SQServerVersions.SQ_5_3_OR_HIGHER);

    Map<String, String> map = SonarBuildWrapper.createVars(installation);

    assertThat(map).containsEntry("SONAR_HOST_URL", "http://localhost:9001");
    assertThat(map).containsEntry("SONAR_CONFIG_NAME", "local");
    // variable in the value should be resolved
    assertThat(map).containsEntry("SONAR_AUTH_TOKEN", "local");
    assertThat(map).containsEntry("SONAR_LOGIN", "");
    assertThat(map).containsEntry("SONAR_PASSWORD", "");
    assertThat(map).containsEntry("SONAR_JDBC_URL", "");
    assertThat(map).containsEntry("SONAR_JDBC_USERNAME", "");
    assertThat(map).containsEntry("SONAR_JDBC_PASSWORD", "");
    assertThat(map).containsEntry("SONAR_MAVEN_GOAL", "sonar:sonar");
    assertThat(map).containsEntry("SONAR_EXTRA_PROPS", "-Dkey=value -X");

    assertThat(map).containsEntry("SONARQUBE_SCANNER_PARAMS", "{ \"sonar.host.url\" : \"http:\\/\\/localhost:9001\", \"sonar.login\" : \"local\"}");
  }

  @Test
  public void testEnvironment51() {
    installation = createTestInstallation(SQServerVersions.SQ_5_1_OR_LOWER);

    Map<String, String> map = SonarBuildWrapper.createVars(installation);

    assertThat(map).containsEntry("SONAR_HOST_URL", "http://localhost:9001");
    assertThat(map).containsEntry("SONAR_CONFIG_NAME", "local");
    // variable in the value should be resolved
    assertThat(map).containsEntry("SONAR_AUTH_TOKEN", "");
    assertThat(map).containsEntry("SONAR_LOGIN", "local");
    assertThat(map).containsEntry("SONAR_PASSWORD", "password");
    assertThat(map).containsEntry("SONAR_JDBC_URL", "");
    assertThat(map).containsEntry("SONAR_JDBC_USERNAME", "sonar");
    assertThat(map).containsEntry("SONAR_JDBC_PASSWORD", "sonar");
    assertThat(map).containsEntry("SONAR_MAVEN_GOAL", "sonar:sonar");
    assertThat(map).containsEntry("SONAR_EXTRA_PROPS", "-Dkey=value -X");
  }

  @Test
  public void testEnvironmentMojoVersion() {
    installation = new SonarInstallation(null, null, null, null, null, null, null, "2.0", null, null, null, null, null);

    Map<String, String> map = SonarBuildWrapper.createVars(installation);

    assertThat(map).containsEntry("SONAR_MAVEN_GOAL", "org.codehaus.mojo:sonar-maven-plugin:2.0:sonar");
  }

  @Test
  public void failOnInvalidInstallationEnvironment() throws Exception {
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
  public void decorateWithoutInstallation() throws IOException, InterruptedException {
    // no installation
    AbstractBuild<?, ?> build = mock(AbstractBuild.class);
    OutputStream out = wrapper.decorateLogger(build, null);
    assertThat(out).isNull();

    verifyZeroInteractions(build);
  }

  @Test
  public void testDisableBuildWrapper() {
    configureSonar(createTestInstallation());
    enableBuildWrapper(false);

    assertThat(((DescriptorImpl) wrapper.getDescriptor()).isApplicable(mock(AbstractProject.class))).isFalse();

    enableBuildWrapper(true);

    assertThat(((DescriptorImpl) wrapper.getDescriptor()).isApplicable(mock(AbstractProject.class))).isTrue();
  }

  @Test
  public void testBuild() throws Exception {
    // set up a free style project with our wrapper that will execute CaptureVarsBuilder
    configureSonar(installation);
    CaptureVarsBuilder b = new CaptureVarsBuilder();
    FreeStyleProject project = setupFreeStyleProject(b);
    project.getBuildWrappersList().add(wrapper);
    Run<?, ?> build = build(project);

    assertThat(build.getResult()).isEqualTo(Result.SUCCESS);

    // ensure that vars were injected to the job
    assertThat(b.vars).containsEntry("SONAR_HOST_URL", "http://localhost:9001");
    assertThat(b.vars).containsEntry("SONAR_LOGIN", "local");
    assertThat(b.vars).containsEntry("SONAR_PASSWORD", "password");
    assertThat(b.vars).containsEntry("SONAR_EXTRA_PROPS", "-Dkey=value -X");

    // job's log should have passwords masked
    assertThat(build.getLog(1000)).contains("the pass is: ******");
    assertThat(build.getActions(SonarAnalysisAction.class)).hasSize(1);
  }

  @Test
  public void testFailedBuild() throws Exception {
    // set up a free style project with our wrapper that will execute CaptureVarsBuilder
    configureSonar(installation);
    CaptureVarsBuilder b = new CaptureVarsBuilder(true);
    FreeStyleProject project = setupFreeStyleProject(b);
    project.getBuildWrappersList().add(wrapper);
    Run<?, ?> build = build(project);

    assertThat(build.getResult()).isEqualTo(Result.FAILURE);

    assertThat(build.getLog(1000)).contains("the pass is: ******");
    assertThat(build.getActions(SonarAnalysisAction.class)).hasSize(1);
  }

  private static class CaptureVarsBuilder extends TestBuilder {
    private boolean fail;
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
      vars = new HashMap<String, String>(env);
      listener.getLogger().println("the pass is: password");
      if (fail) {
        throw new AbortException("asd");
      }
      return true;
    }
  }

  private void enableBuildWrapper(boolean enable) {
    j.jenkins.getDescriptorByType(SonarGlobalConfiguration.class).setBuildWrapperEnabled(enable);
  }

  private static SonarInstallation createTestInstallation() {
    return new SonarInstallation("local", "http://localhost:9001", SQServerVersions.SQ_5_2, null, null, null, null,
      null, "-X", new TriggersConfig(), "$SONAR_CONFIG_NAME", "password", "key=value");
  }

  private static SonarInstallation createTestInstallation(String version) {
    return new SonarInstallation("local", "http://localhost:9001", version, "$SONAR_CONFIG_NAME", null, null, null,
      null, "-X", new TriggersConfig(), "$SONAR_CONFIG_NAME", "password", "key=value");
  }
}
