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

import hudson.maven.MavenModuleSet;
import hudson.maven.local_repo.PerJobLocalRepositoryLocator;
import hudson.model.Cause;
import hudson.model.FreeStyleProject;
import hudson.model.Project;
import hudson.model.Result;
import hudson.model.Run;
import org.junit.Test;
import org.jvnet.hudson.test.MockBuilder;

import static org.mockito.Mockito.spy;

/**
 * @author Evgeny Mandrikov
 */
public class BaseTest extends SonarTestCase {
  /**
   * No sonar installations defined.
   *
   * @throws Exception if something is wrong
   */
  @Test
  public void testNoSonarInstallation() throws Exception {
    FreeStyleProject project = setupFreeStyleProjectWithSonarRunner();
    project.getPublishersList().add(newSonarPublisherForFreeStyleProject(ROOT_POM));
    Run<?, ?> build = build(project);

    assertNoSonarExecution(build, Messages.SonarInstallation_NoMatchInstallation(SONAR_INSTALLATION_NAME, 0));
  }

  /**
   * Maven Project.
   * <ul>
   * <li>SONARPLUGINS-19: Maven "-B" option (batch mode)</li>
   * <li>SONARPLUGINS-73: Root POM</li>
   * <li>SONARPLUGINS-101: Private Repository</li>
   * <li>SONARPLUGINS-253: Maven "-e" option</li>
   * <li>SONARPLUGINS-263: Path to POM with spaces</li>
   * <li>SONARPLUGINS-326: Use alternate settings file</li>
   * </ul>
   *
   * @throws Exception if something is wrong
   */
  @Test
  public void testMavenProject() throws Exception {
    configureDefaultMaven();
    configureDefaultSonar();
    String pomName = "space test/root-pom.xml";
    MavenModuleSet project = setupSonarMavenProject(pomName);
    project.setAlternateSettings("/settings.xml");
    project.setLocalRepository(new PerJobLocalRepositoryLocator());
    Run<?, ?> build = build(project);
  }

  @Test
  public void testFreeStyleProjectWithSonarRunnerStep() throws Exception {
    configureDefaultSonarRunner(false);
    configureDefaultSonar();
    FreeStyleProject project = setupFreeStyleProjectWithSonarRunner();
    Run<?, ?> build = build(project);

    assertSonarExecution(build, "This is a fake Runner", true);
  }

  @Test
  public void testFreeStyleProjectWithBrokenSonarRunnerStep() throws Exception {
    configureDefaultSonarRunner(true);
    configureDefaultSonar();
    FreeStyleProject project = setupFreeStyleProjectWithSonarRunner();
    Run<?, ?> build = build(project);

    assertSonarExecution(build, "This is a fake Runner", false);
  }

  protected void setBuildResult(Project<?, ?> project, Result result) throws Exception {
    project.getBuildersList().clear();
    project.getBuildersList().add(new MockBuilder(result));
  }

  public static class CustomCause extends Cause.UserIdCause {
  }

  /**
   * SONARPLUGINS-48: Hide the token from the standard output
   *
   * @throws Exception if something wrong
   */
  @Test
  public void testPassword() throws Exception {
    configureDefaultSonarRunner(false);
    configureSecuredSonar();

    FreeStyleProject project = setupFreeStyleProjectWithSonarRunner();
    Run<?, ?> build = build(project);

    assertLogContains("sonar-runner", build);
    assertLogContains("-Dsonar.host.url=http://example.org:9999/sonar ********", build); // check masked token is in logs
    assertLogDoesntContains("secret", build);
  }

  private void configureSecuredSonar() {
    SonarInstallation installation = spy(new SonarInstallation(
            SONAR_INSTALLATION_NAME,
            SONAR_HOST,
            "token", null, null, null, null, null));
    addCredential("token", "secret");
    configureSonar(installation);
  }
}
