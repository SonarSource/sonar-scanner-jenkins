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

import com.google.common.annotations.VisibleForTesting;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Node;
import hudson.plugins.sonar.utils.ExtendedArgumentListBuilder;
import hudson.scm.SCM;
import hudson.util.ArgumentListBuilder;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SonarRunnerBuilderTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private File moduleDir;
  private ExtendedArgumentListBuilder argsBuilder;
  private MyBuild<?, ?> build;
  private BuildListener listener;
  private EnvVars env;
  private File workspace;
  private ArgumentListBuilder args;

  @Before
  public void prepareMockWorkspace() throws IOException {
    workspace = temp.newFolder();
    moduleDir = new File(workspace, "trunk");
    FileUtils.forceMkdir(moduleDir);
    args = new ArgumentListBuilder();
    argsBuilder = new ExtendedArgumentListBuilder(args, false);
    AbstractProject<?, ?> p = mock(AbstractProject.class);
    SCM scm = mock(SCM.class);
    FilePath workspacePath = new FilePath(workspace);
    when(scm.getModuleRoot(eq(workspacePath), any(AbstractBuild.class))).thenReturn(new FilePath(moduleDir));
    when(p.getScm()).thenReturn(scm);
    build = new MyBuild(p);
    build.setWorkspace(workspacePath);
    listener = mock(BuildListener.class);
    env = new EnvVars();
  }

  @After
  public void cleanWorkspace() {
    FileUtils.deleteQuietly(workspace);
  }

  @Test
  public void shouldBeEmptyInsteadOfNull() {
    SonarRunnerBuilder builder = new SonarRunnerBuilder(null, null, null, null, null, null, null);
    assertEmptyInsteadOfNull(builder);
  }

  private void assertEmptyInsteadOfNull(SonarRunnerBuilder builder) {
    assertThat(builder.getInstallationName()).isEmpty();
    assertThat(builder.getJavaOpts()).isEmpty();
    assertThat(builder.getProject()).isEmpty();
    assertThat(builder.getProperties()).isEmpty();
    assertThat(builder.getSonarRunnerName()).isEmpty();
  }

  @Test
  public void shouldPopulateProjectSettingsParameter() throws IOException, InterruptedException {
    File projectSettings = new File(moduleDir, "myCustomProjectSettings.properties");
    projectSettings.createNewFile();

    SonarRunnerBuilder builder = new SonarRunnerBuilder(null, null, "myCustomProjectSettings.properties", null, null, null, null);
    builder.populateConfiguration(argsBuilder, build, listener, env, null);

    assertThat(args.toStringWithQuote())
      .contains("-Dsonar.projectBaseDir=" + moduleDir)
      .contains("-Dproject.settings=" + projectSettings);
  }

  @Test
  public void shouldPopulateSonarLoginPasswordParameters() throws IOException, InterruptedException {
    SonarInstallation installation = mock(SonarInstallation.class);
    when(installation.getServerUrl()).thenReturn("hostUrl");
    when(installation.getDatabaseUrl()).thenReturn("databaseUrl");
    when(installation.getDatabaseLogin()).thenReturn("login");
    when(installation.getDatabasePassword()).thenReturn("password");
    when(installation.getSonarLogin()).thenReturn("sonarlogin");
    when(installation.getSonarPassword()).thenReturn("sonarpassword");

    SonarRunnerBuilder builder = new SonarRunnerBuilder(null, null, null, null, null, null, null);
    builder.populateConfiguration(argsBuilder, build, listener, env, installation);

    assertThat(args.toStringWithQuote())
      .contains("-Dsonar.login=sonarlogin")
      .contains("-Dsonar.password=sonarpassword");
  }

  /**
   * It is not possible to mock AbstractBuild because interesting methods like getWorkspace are final so I am creating a custom subclass
   * @author julien
   *
   */
  private class MyBuild<P extends AbstractProject<P, R>, R extends AbstractBuild<P, R>> extends AbstractBuild<P, R> {

    protected MyBuild(P job) throws IOException {
      super(job);
    }

    @Override
    @VisibleForTesting
    public void setWorkspace(FilePath ws) {
      super.setWorkspace(ws);
    }

    @Override
    public Node getBuiltOn() {
      Node n = mock(Node.class);
      when(n.createPath(Mockito.anyString())).thenAnswer(new Answer<FilePath>() {
        public FilePath answer(InvocationOnMock invocation) {
          Object[] args = invocation.getArguments();
          return new FilePath(new File(args[0].toString()));
        }
      });
      return n;
    }

    @Override
    public void run() {

    }

  }

}
