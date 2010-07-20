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
import hudson.model.*;
import hudson.plugins.sonar.model.TriggersConfig;
import hudson.triggers.SCMTrigger;
import hudson.triggers.TimerTrigger;
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
  public void testNoSonarInstallation() throws Exception {
    FreeStyleProject project = setupFreeStyleProject();
    project.getPublishersList().add(newSonarPublisherForFreeStyleProject(ROOT_POM));
    AbstractBuild build = build(project);

    assertNoSonarExecution(build, Messages.SonarPublisher_NoInstallation("default", 0));
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
  public void testMavenProject() throws Exception {
    configureDefaultMaven();
    configureDefaultSonar();
    String pomName = "space test/root-pom.xml";
    MavenModuleSet project = setupMavenProject(pomName);
    project.setAlternateSettings("settings.xml");
    project.setUsePrivateRepository(true);
    AbstractBuild build = build(project);

    String repo = build.getWorkspace().child(".repository").getRemote();
    // TODO Check that there is no POM-generation for Maven project
    assertSonarExecution(build, "-f \"" + getPom(build, pomName) + "\" -Dmaven.repo.local=" + repo + " -s settings.xml");
  }

  /**
   * Free Style Project.
   * <ul>
   * <li>SONARPLUGINS-19: Maven "-B" option (batch mode)</li>
   * <li>SONARPLUGINS-73: Root POM</li>
   * <li>SONARPLUGINS-253: Maven "-e" option</li>
   * <li>SONARPLUGINS-263: Path to POM with spaces</li>
   * </ul>
   *
   * @throws Exception if something is wrong
   */
  public void testFreeStyleProject() throws Exception {
    configureDefaultMaven();
    configureDefaultSonar();
    String pomName = "space test/sonar-pom.xml";
    FreeStyleProject project = setupFreeStyleProject(pomName);
    project.getPublishersList().add(newSonarPublisherForFreeStyleProject(pomName));
    AbstractBuild build = build(project);

    assertSonarExecution(build, "-f \"" + getPom(build, pomName) + "\"");
    // Check that POM generated
    assertTrue(build.getWorkspace().child(pomName).exists());
  }

  /**
   * SONARPLUGINS-153, SONARPLUGINS-216: Triggers
   * SONARPLUGINS-378
   * SONARPLUGINS-461
   *
   * @throws Exception if something wrong
   */
  public void testTriggers() throws Exception {
    configureDefaultMaven();
    configureDefaultSonar();
    FreeStyleProject project = setupFreeStyleProject();
    TriggersConfig triggers = project.getPublishersList().get(SonarPublisher.class).getTriggers();
    triggers.setUserBuilds(false);
    triggers.setScmBuilds(false);
    triggers.setTimerBuilds(false);
    triggers.setSnapshotDependencyBuilds(false);
    triggers.setSkipIfBuildFails(true);
    AbstractBuild build;

    setBuildResult(project, Result.SUCCESS);
    // SONARPLUGINS-378
    build = build(project, new CustomCause(), null);
    assertNoSonarExecution(build, Messages.SonarPublisher_UserBuild());
    // Disable sonar on user build command execution
    build = build(project, new Cause.UserCause(), null);
    assertNoSonarExecution(build, Messages.SonarPublisher_UserBuild());
    // Disable sonar on SCM build
    build = build(project, new SCMTrigger.SCMTriggerCause(), null);
    assertNoSonarExecution(build, Messages.SonarPublisher_SCMBuild());
    // Disable sonar on Timer build
    build = build(project, new TimerTrigger.TimerTriggerCause(), null);
    assertNoSonarExecution(build, Messages.SonarPublisher_TimerBuild());
    // Disable sonar on Upstream build
    build = build(project, new Cause.UpstreamCause((Run) build), null);
    assertNoSonarExecution(build, Messages.SonarPublisher_SnapshotDepBuild());
    // Disable sonar on build failure
    setBuildResult(project, Result.FAILURE);
    build = build(project);
    assertNoSonarExecution(build, Messages.SonarPublisher_BadBuildStatus(Result.FAILURE));
    // Enable sonar on build failure
    triggers.setSkipIfBuildFails(false);
    build = build(project);
    assertSonarExecution(build);
    // Disable sonar if build status worse than failure
    setBuildResult(project, Result.ABORTED);
    build = build(project);
    assertNoSonarExecution(build, Messages.SonarPublisher_BadBuildStatus(Result.ABORTED));
    // Enable sonar on build unstable
    setBuildResult(project, Result.UNSTABLE);
    build = build(project);
    assertSonarExecution(build);
    // Enable sonar on build success
    setBuildResult(project, Result.SUCCESS);
    build = build(project);
    assertSonarExecution(build);
  }

  protected void setBuildResult(Project project, Result result) throws Exception {
    project.getBuildersList().clear();
    project.getBuildersList().add(new MockBuilder(result));
  }

  public static class CustomCause extends Cause.UserCause {
  }

  /**
   * SONARPLUGINS-48: Hide the user/password from the standard output
   *
   * @throws Exception if something wrong
   */
  public void testPassword() throws Exception {
    configureDefaultMaven();
    configureSecuredSonar();
    MavenModuleSet project = setupMavenProject();
    AbstractBuild build = build(project);

    assertLogContains("sonar:sonar", build);
    assertLogDoesntContains("-Dsonar.jdbc.username=dbuser", build);
    assertLogDoesntContains("-Dsonar.jdbc.password=dbpassword", build);
  }

  private void configureSecuredSonar() {
    configureSonar(new SonarInstallation(
        SONAR_INSTALLATION_NAME,
        false,
        SONAR_HOST, null,
        "jdbc:mysql://dbhost:dbport/sonar?useUnicode=true&characterEncoding=utf8",
        "com.mysql.jdbc.Driver",
        "dbuser", "dbpassword",
        null, null, null
    ));
  }
}
