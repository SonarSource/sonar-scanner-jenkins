package hudson.plugins.sonar;

import hudson.model.*;
import hudson.triggers.SCMTrigger;
import hudson.triggers.TimerTrigger;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.Email;
import org.jvnet.hudson.test.FailureBuilder;

/**
 * TODO:
 * <ul>
 * <li>SONARPLUGINS-101 - Private Repository</li>
 * <li>Start Sonar???</li>
 * </ul>
 *
 * @author Evgeny Mandrikov
 */
public class BaseTest extends SonarTestCase {
  /**
   * Maven Project.
   * <ul>
   * <li>SONARPLUGINS-19: Maven "-B" option (batch mode)</li>
   * <li>SONARPLUGINS-73: Root POM</li>
   * <li>SONARPLUGINS-253: Maven "-e" option</li>
   * <li>SONARPLUGINS-263: Path to POM with spaces</li>
   * </ul>
   *
   * @throws Exception if something is wrong
   */
  public void testMavenProject() throws Exception {
    /* FIXME disabled
    configureDefaultMaven();
    configureDefaultSonar();
    String pomName = "space test/root-pom.xml";
    MavenModuleSet project = setupMavenProject(pomName);
    MavenModuleSetBuild build = build(project);

    // TODO Check that there is no POM-generation for Maven project
    assertSonarExecution(build, "-f \"" + pomName + "\""
        + " -Dsonar.jdbc.password=" + DATABASE_PASSWORD
        + " -Dsonar.host.url=" + SONAR_HOST
    );
    */
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
    FreeStyleBuild build = build(project);

    assertSonarExecution(build, "-f \"" + pomName + "\""
        + " -Dsonar.jdbc.password=" + DATABASE_PASSWORD
        + " -Dsonar.host.url=" + SONAR_HOST
    );
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
    FreeStyleBuild build;
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
    build = build(project, null);
    assertNoSonarExecution(build, Messages.SonarPublisher_BadBuildStatus(Result.FAILURE));
  }

  /**
   * TODO SONARPLUGINS-123: Ampersand problem.
   *
   * @throws Exception if something is wrong
   */
  @Bug(123)
  @Email("http://old.nabble.com/Hudson-Builds-Fail-With-1.9-tt23859002.html")
  public void testAmpersand() throws Exception {
    /*
    configureDefaultMaven();
    configureSonar(new SonarInstallation(
        SONAR_INSTALLATION_NAME,
        false,
        SONAR_HOST,
        "jdbc:mysql://dbhost:dbport/sonar?useUnicode=true&characterEncoding=utf8",
        "com.mysql.jdbc.Driver",
        "sonar",
        "sonar",
        null
    ));
    MavenModuleSet project = setupMavenProject();
    MavenModuleSetBuild build = build(project);

    assertLogContains("-f pom.xml -Dsonar.jdbc.driver=com.mysql.jdbc.Driver -Dsonar.jdbc.username=sonar -Dsonar.jdbc.password=sonar -Dsonar.jdbc.url=jdbc:mysql://dbhost:dbport/sonar?useUnicode=true&characterEncoding=utf8 -Dsonar.host.url=http://sonarhost:sonarport", build);
    */
  }
}
