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

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.model.Run;
import hudson.model.Run.RunnerAbortedException;
import hudson.plugins.sonar.SonarBuildWrapper.DescriptorImpl;
import hudson.plugins.sonar.SonarBuildWrapper.SonarEnvironment;
import hudson.plugins.sonar.model.TriggersConfig;
import hudson.util.IOUtils;
import org.junit.Test;
import org.jvnet.hudson.test.TestBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SonarBuildWrapperTest extends SonarTestCase {

  @Test
  public void testInstance() {
    SonarBuildWrapper wrapper = new SonarBuildWrapper("local");

    assertThat(wrapper.getInstallationName()).isEqualTo("local");
    assertThat(wrapper.getInstallation()).isNull();
  }

  @Test
  public void testDescriptor() {
    SonarBuildWrapper wrapper = new SonarBuildWrapper("local");
    DescriptorImpl desc = wrapper.getDescriptor();

    assertThat(desc.getDisplayName()).isEqualTo("Prepare SonarQube Scanner environment");
    assertThat(desc.getHelpFile()).isEqualTo("/plugin/sonar/help-buildWrapper.html");
    assertThat(desc.isApplicable(mock(AbstractProject.class))).isTrue();

    assertThat(desc.getSonarInstallations()).isEmpty();
  }

  public void testLogging() throws RunnerAbortedException, IOException, InterruptedException {
    SonarBuildWrapper wrapper = new SonarBuildWrapper("local");
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    OutputStream os = wrapper.decorateLogger(mock(AbstractBuild.class), bos);
    IOUtils.write("test password\n", os);
    assertThat(new String(bos.toByteArray())).isEqualTo("test password\n");

    // with a SQ instance configured (should mask passwords)
    configureSonar(createTestInstallation());
    bos = new ByteArrayOutputStream();
    os = wrapper.decorateLogger(mock(AbstractBuild.class), bos);
    IOUtils.write("test password\n", os);
    assertThat(new String(bos.toByteArray())).isEqualTo("test ******\n");
  }

  @Test
  public void testEnvironment() {
    SonarBuildWrapper wrapper = new SonarBuildWrapper("local");
    SonarInstallation installation = createTestInstallation();
    PrintStream stream = mock(PrintStream.class);
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
    assertThat(map).containsEntry("SONAR_ADDITIONAL", "");

    // existing entries still there
    assertThat(map).containsEntry("key", "value");
    verify(stream).println(contains("Injecting SonarQube environment variables"));
  }

  @Test
  public void testBuild() throws Exception {
    // set up a free style project with our wrapper that will execute CaptureVarsBuilder
    SonarBuildWrapper wrapper = new SonarBuildWrapper("local");
    configureSonar(createTestInstallation());
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
