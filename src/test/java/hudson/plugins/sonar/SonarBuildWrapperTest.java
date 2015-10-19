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

import org.apache.commons.io.IOUtils;
import hudson.model.Run;
import org.junit.Before;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.model.Run.RunnerAbortedException;
import hudson.plugins.sonar.SonarBuildWrapper.DescriptorImpl;
import hudson.plugins.sonar.SonarBuildWrapper.SonarEnvironment;
import hudson.plugins.sonar.model.TriggersConfig;
import org.junit.Test;
import org.jvnet.hudson.test.TestBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
  public void testDescriptor() {
    DescriptorImpl desc = wrapper.getDescriptor();

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
  public void maskDefaultPassword() throws IOException, InterruptedException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    configureSonar(new SonarInstallation("local", false, "http://localhost:9001", null, null, null,
      null, null, new TriggersConfig(), "$SONAR_CONFIG_NAME", null));
    
    OutputStream os = wrapper.decorateLogger(mock(AbstractBuild.class), bos);
    IOUtils.write("test sonar\ntest something\n", os);
    assertThat(new String(bos.toByteArray())).isEqualTo("test ******\ntest something\n");
  }

  @Test
  public void testEnvironment() {
    SonarEnvironment env = wrapper.new SonarEnvironment(installation, stream);

    Map<String, String> map = new HashMap<String, String>();
    map.put("key", "value");
    env.buildEnvVars(map);

    assertThat(map).containsEntry("SONAR_HOST_URL", "http://localhost:9001");
    assertThat(map).containsEntry("SONAR_CONFIG_NAME", "local");
    // variable in the value should be resolved
    assertThat(map).containsEntry("SONAR_LOGIN", "local");
    assertThat(map).containsEntry("SONAR_PASSWORD", "password");
    assertThat(map).containsEntry("SONAR_JDBC_URL", "");
    assertThat(map).containsEntry("SONAR_JDBC_USERNAME", "");
    assertThat(map).containsEntry("SONAR_JDBC_PASSWORD", "");
    assertThat(map).containsEntry("SONAR_EXTRA_PROPS", "");

    // existing entries still there
    assertThat(map).containsEntry("key", "value");
    verify(stream).println(contains("Injecting SonarQube environment variables"));
  }

  @Test
  public void dontShowDisabledInstallations() {
    configureSonar(installation);
    // the descriptor is used to display the configuration page
    assertThat(wrapper.getDescriptor().getSonarInstallations()).hasSize(1);

    SonarInstallation installation2 = new SonarInstallation("local2", true, "http://localhost:9001", null, null, null,
      null, null, new TriggersConfig(), "$SONAR_CONFIG_NAME", "password");
    configureSonar(installation2);
    // disabled not shown
    assertThat(wrapper.getDescriptor().getSonarInstallations()).hasSize(0);
  }

  @Test
  public void failOnInvalidInstallationEnvironment() throws Exception {
    // non existing installation
    BuildListener listener = mock(BuildListener.class);
    when(listener.getLogger()).thenReturn(stream);
    wrapper.setUp(mock(AbstractBuild.class), mock(Launcher.class), listener);
    verify(listener).fatalError(contains("does not match"));
  }

  @Test
  public void failOnDisabledInstallationEnvironment() throws Exception {
    // disabled installation
    configureSonar(new SonarInstallation("local", true, "http://localhost:9001", null, null, null,
      null, null, new TriggersConfig(), "$SONAR_CONFIG_NAME", "password"));

    BuildListener listener = mock(BuildListener.class);
    when(listener.getLogger()).thenReturn(stream);
    wrapper.setUp(mock(AbstractBuild.class), mock(Launcher.class), listener);
    verify(listener).fatalError(contains("installation assigned to this job is disabled"));
  }

  @Test
  public void testDisableBuildWrapper() {
    configureSonar(createTestInstallation());
    enableBuildWrapper(false);

    assertThat(wrapper.getDescriptor().isApplicable(mock(AbstractProject.class))).isFalse();

    enableBuildWrapper(true);

    assertThat(wrapper.getDescriptor().isApplicable(mock(AbstractProject.class))).isTrue();
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

    // job's log should have passwords masked
    assertThat(build.getLog(1000)).contains("the pass is: ******");
  }

  private static class CaptureVarsBuilder extends TestBuilder {
    Map<String, String> vars;

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
      EnvVars env = build.getEnvironment(listener);
      vars = new HashMap<String, String>(env);
      listener.getLogger().println("the pass is: password");
      return true;
    }
  }

  private static SonarInstallation createTestInstallation() {
    return new SonarInstallation("local", false, "http://localhost:9001", null, null, null,
      null, null, new TriggersConfig(), "$SONAR_CONFIG_NAME", "password");
  }
}
