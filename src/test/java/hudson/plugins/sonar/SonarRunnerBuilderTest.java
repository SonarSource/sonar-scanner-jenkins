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

import com.google.common.annotations.VisibleForTesting;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Node;
import hudson.model.Run;
import hudson.plugins.sonar.client.HttpClient;
import hudson.plugins.sonar.client.WsClient;
import hudson.plugins.sonar.utils.ExtendedArgumentListBuilder;
import hudson.scm.SCM;
import hudson.util.ArgumentListBuilder;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SonarRunnerBuilderTest extends SonarTestCase {

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
    SonarRunnerBuilder builder = new SonarRunnerBuilder();
    assertEmptyInsteadOfNull(builder);
  }

  @Test
  public void additionalArgs() {
    ArgumentListBuilder args = new ArgumentListBuilder();
    SonarInstallation inst = new SonarInstallation(null, null, null, null, null, null, "-Y", "key=value", null);
    SonarRunnerBuilder builder = new SonarRunnerBuilder();
    builder.setProject("myCustomProjectSettings.properties");
    builder.setAdditionalArguments("-X -e");
    builder.addAdditionalArguments(args, inst);
    assertThat(args.toString()).isEqualTo("-Y -Dkey=value -X -e");

    builder = new SonarRunnerBuilder();
    builder.setProject("myCustomProjectSettings.properties");
    builder.setAdditionalArguments("-X");
    args.clear();
    builder.addAdditionalArguments(args, inst);
    assertThat(args.toString()).isEqualTo("-Y -Dkey=value -X");
  }

  private void assertEmptyInsteadOfNull(SonarRunnerBuilder builder) {
    assertThat(builder.getInstallationName()).isEmpty();
    assertThat(builder.getJavaOpts()).isEmpty();
    assertThat(builder.getProject()).isEmpty();
    assertThat(builder.getProperties()).isEmpty();
    assertThat(builder.getSonarScannerName()).isEmpty();
    assertThat(builder.getAdditionalArguments()).isEmpty();
  }

  @Test
  public void shouldPopulateProjectSettingsParameter() throws IOException, InterruptedException {
    File projectSettings = new File(moduleDir, "myCustomProjectSettings.properties");
    projectSettings.createNewFile();

    SonarRunnerBuilder builder = new SonarRunnerBuilder();
    builder.setProject("myCustomProjectSettings.properties");
    builder.populateConfiguration(argsBuilder, build, build.getWorkspace(), listener, env, null, null);

    assertThat(args.toStringWithQuote())
      .contains("-Dsonar.projectBaseDir=" + moduleDir)
      .contains("-Dproject.settings=" + projectSettings);
  }

  @Test
  public void populateConfiguration_whenSQVersionLowerThan10_shouldPopulateSonarLogin() throws IOException, InterruptedException {
    SonarInstallation installation = mock(SonarInstallation.class);
    when(installation.getServerUrl()).thenReturn("hostUrl");
    when(installation.getServerAuthenticationToken(any(Run.class))).thenReturn("token");
    HttpClient client = mockServerVersion(installation, "9.9");

    SonarRunnerBuilder builder = new SonarRunnerBuilder();
    builder.populateConfiguration(argsBuilder, build, build.getWorkspace(), listener, env, installation, client);

    assertThat(args.toStringWithQuote())
      .contains("-Dsonar.login=token");
  }

  @Test
  public void populateConfiguration_whenSQVersionHigherThan10_shouldPopulateSonarToken() throws IOException, InterruptedException {
    SonarInstallation installation = mock(SonarInstallation.class);
    when(installation.getServerUrl()).thenReturn("hostUrl");
    when(installation.getServerAuthenticationToken(any(Run.class))).thenReturn("token");
    HttpClient client = mockServerVersion(installation, "10.0");

    SonarRunnerBuilder builder = new SonarRunnerBuilder();
    builder.populateConfiguration(argsBuilder, build, build.getWorkspace(), listener, env, installation, client);

    assertThat(args.toStringWithQuote())
      .contains("-Dsonar.token=token");
  }

  @Test
  public void populateConfiguration_whenInstallationHasNoUrl_shouldPopulateSonarLogin() throws IOException, InterruptedException {
    SonarInstallation installation = mock(SonarInstallation.class);
    when(installation.getServerAuthenticationToken(any(Run.class))).thenReturn("token");
    HttpClient client = mockServerVersion(installation, "10.0");

    SonarRunnerBuilder builder = new SonarRunnerBuilder();
    builder.populateConfiguration(argsBuilder, build, build.getWorkspace(), listener, env, installation, client);

    assertThat(args.toStringWithQuote())
      .contains("-Dsonar.login=token");
  }

  private static HttpClient mockServerVersion(SonarInstallation installation, String version) {
    HttpClient client = mock(HttpClient.class);
    when(client.getHttp(installation.getServerUrl() + WsClient.API_VERSION, null)).thenReturn(version);
    return client;
  }

  /**
   * It is not possible to mock AbstractBuild because interesting methods like getWorkspace are final so I am creating a custom subclass
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
