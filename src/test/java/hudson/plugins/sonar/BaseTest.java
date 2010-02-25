package hudson.plugins.sonar;

import hudson.maven.MavenModuleSet;
import hudson.model.*;
import hudson.plugins.sonar.model.TriggersConfig;
import hudson.triggers.SCMTrigger;
import hudson.triggers.TimerTrigger;
import org.jvnet.hudson.test.FailureBuilder;

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
    assertSonarExecution(build, "-f \"" + pomName + "\" -Dmaven.repo.local=" + repo + " -s settings.xml");
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

    assertSonarExecution(build, "-f \"" + pomName + "\"");
    // Check that POM generated
    assertTrue(build.getWorkspace().child(pomName).exists());
  }

  /**
   * SONARPLUGINS-153, SONARPLUGINS-216: Triggers
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
    project.getBuildersList().add(new FailureBuilder());
    build = build(project);
    assertNoSonarExecution(build, Messages.SonarPublisher_BadBuildStatus(Result.FAILURE));
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
        SONAR_HOST,
        "jdbc:mysql://dbhost:dbport/sonar?useUnicode=true&characterEncoding=utf8",
        "com.mysql.jdbc.Driver",
        "dbuser", "dbpassword",
        null, null
    ));
  }
}
