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

import hudson.maven.MavenModuleSet;
import hudson.maven.local_repo.PerJobLocalRepositoryLocator;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.FreeStyleProject;
import hudson.model.Project;
import org.junit.Test;
import org.jvnet.hudson.test.MockBuilder;

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
    FreeStyleProject project = setupFreeStyleProject();
    project.getPublishersList().add(newSonarPublisherForFreeStyleProject(ROOT_POM));
    AbstractBuild<?, ?> build = build(project);

    assertNoSonarExecution(build, Messages.SonarPublisher_NoMatchInstallation(SONAR_INSTALLATION_NAME, 0));
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
    MavenModuleSet project = setupMavenProject(pomName);
    project.setAlternateSettings("/settings.xml");
    project.setLocalRepository(new PerJobLocalRepositoryLocator());
    AbstractBuild<?, ?> build = build(project);
  }

  @Test
  public void testFreeStyleProjectWithSonarRunnerStep() throws Exception {
    configureDefaultSonarRunner(false);
    configureDefaultSonar();
    FreeStyleProject project = setupFreeStyleProject();
    AbstractBuild<?, ?> build = build(project);

    assertSonarExecution(build, "This is a fake Runner", true);
  }

  @Test
  public void testFreeStyleProjectWithBrokenSonarRunnerStep() throws Exception {
    configureDefaultSonarRunner(true);
    configureDefaultSonar();
    FreeStyleProject project = setupFreeStyleProject();
    AbstractBuild<?, ?> build = build(project);

    assertSonarExecution(build, "This is a fake Runner", false);
  }

  protected void setBuildResult(Project<?, ?> project, Result result) throws Exception {
    project.getBuildersList().clear();
    project.getBuildersList().add(new MockBuilder(result));
  }

  public static class CustomCause extends Cause.UserIdCause {
  }

  /**
   * SONARPLUGINS-48: Hide the user/password from the standard output
   *
   * @throws Exception if something wrong
   */
  @Test
  public void testPassword() throws Exception {
    configureDefaultSonarRunner(false);
    configureSecuredSonar();
    FreeStyleProject project = setupFreeStyleProject();
    AbstractBuild<?, ?> build = build(project);

    assertLogContains("sonar-runner", build);
    assertLogDoesntContains("sonar.jdbc.username", build);
    assertLogDoesntContains("sonar.jdbc.password", build);
  }

  private void configureSecuredSonar() {
    configureSonar(new SonarInstallation(
      SONAR_INSTALLATION_NAME,
      false,
      SONAR_HOST,
      "jdbc:mysql://dbhost:dbport/sonar?useUnicode=true&characterEncoding=utf8",
      "dbuser", "dbpassword",
      null, null, null, null, null));
  }
}
