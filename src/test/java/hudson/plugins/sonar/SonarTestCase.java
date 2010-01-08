package hudson.plugins.sonar;

import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.*;
import hudson.scm.NullSCM;
import hudson.tasks.Maven;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.SingleFileSCM;

import java.io.File;

import static org.mockito.Mockito.spy;

/**
 * TODO Set -Dmaven.home to MAVEN_HOME
 *
 * @author Evgeny Mandrikov
 */
public abstract class SonarTestCase extends HudsonTestCase {
  /**
   * Setting this to non-existent host, allows us to avoid intersection with exist Sonar.
   */
  public static final String SONAR_HOST = "http://example.org:9999/sonar";
  public static final String DATABASE_PASSWORD = "password";

  public static final String ROOT_POM = "sonar-pom.xml";
  public static final String SONAR_INSTALLATION_NAME = "default";

  @Override
  protected void setUp() throws Exception {
    System.setProperty("maven.home", "/usr/share/maven-bin-2.2"); // FIXME
    super.setUp();
  }

  @Override
  protected Maven.MavenInstallation configureDefaultMaven() throws Exception {
    File mvn = new File(getClass().getResource("SonarTestCase/maven/bin/mvn").toURI().getPath());
    String home = mvn.getParentFile().getAbsolutePath();
    Maven.MavenInstallation mavenInstallation = new Maven.MavenInstallation("default", home, NO_PROPERTIES);
    hudson.getDescriptorByType(Maven.DescriptorImpl.class).setInstallations(mavenInstallation);
    return mavenInstallation;
  }

  protected SonarInstallation configureDefaultSonar() {
    return configureSonar(
        new SonarInstallation(
            SONAR_INSTALLATION_NAME,
            false,
            SONAR_HOST,
            null, null, null, DATABASE_PASSWORD, // Database Properties
            null // Additinal Properties
        )
    );
  }

  protected SonarInstallation configureSonar(SonarInstallation sonarInstallation) {
    hudson.getDescriptorByType(SonarPublisher.DescriptorImpl.class).setInstallations(sonarInstallation);
    return sonarInstallation;
  }

  protected MavenModuleSet setupMavenProject() throws Exception {
    return setupMavenProject("pom.xml");
  }

  protected MavenModuleSet setupMavenProject(String pomName) throws Exception {
    MavenModuleSet project = createMavenProject("MavenProject");
    // Setup SCM
    project.setScm(new SingleFileSCM(pomName, getClass().getResource("/hudson/plugins/sonar/SonarTestCase/pom.xml")));
    // Setup Maven
    project.setRootPOM(pomName);
    project.setGoals("clean install");
    project.setIsArchivingDisabled(true);
    // Setup Sonar
    project.getPublishersList().add(newSonarPublisherForMavenProject());
    return project;
  }

  protected FreeStyleProject setupFreeStyleProject() throws Exception {
    return setupFreeStyleProject(ROOT_POM);
  }

  protected FreeStyleProject setupFreeStyleProject(String pomName) throws Exception {
    FreeStyleProject project = super.createFreeStyleProject("FreeStyleProject");
    // Setup SCM
    project.setScm(new NullSCM());
    // Setup Sonar
    project.getPublishersList().add(newSonarPublisherForFreeStyleProject(pomName));
    return project;
  }

  protected MavenModuleSetBuild build(final MavenModuleSet project) throws Exception {
    return build(project, null);
  }

  protected FreeStyleBuild build(final FreeStyleProject project) throws Exception {
    return build(project, null);
  }

  protected MavenModuleSetBuild build(final MavenModuleSet project, Result expectedStatus) throws Exception {
    MavenModuleSetBuild build = project.scheduleBuild2(0, new Cause.RemoteCause("", "")).get();
    if (expectedStatus != null) {
      assertBuildStatus(expectedStatus, build);
    }
    return build;
  }

  protected FreeStyleBuild build(FreeStyleProject project, Result expectedStatus) throws Exception {
    return build(project, new Cause.RemoteCause("", ""), expectedStatus);
  }

  protected FreeStyleBuild build(FreeStyleProject project, Cause cause, Result expectedStatus) throws Exception {
    FreeStyleBuild build = project.scheduleBuild2(0, cause).get();
    if (expectedStatus != null) {
      assertBuildStatus(expectedStatus, build);
    }
    return build;
  }

  protected static SonarPublisher newSonarPublisherForMavenProject() {
    SonarPublisher publisher = new SonarPublisher(
        SONAR_INSTALLATION_NAME,
        null,
        false, // Sonar Light
        null, null, null, null,
        null, null, null, null,
        null, null,
        false, false, false, true, true, // Triggers
        null,
        false, // Reuse Reports
        null, null, null, null
    );
    return spy(publisher);
  }

  protected static SonarPublisher newSonarPublisherForFreeStyleProject(String pomName) {
    // TODO Godin: I don't know why, but spy don't work for FreeStyleProject
    return new SonarPublisher(
        SONAR_INSTALLATION_NAME,
        null,
        true, // Sonar Light
        "test", "test",
        "Test", // TODO can be ${JOB_NAME} ?
        "0.1-SNAPSHOT", // Project Information
        "src", // Project SRC Dir 
        null, // Java Version
        "Test project", // Project Description
        null, // Maven OPTS
        "default", // Maven Installation Name
        pomName, // Root POM
        false, false, false, false, true, // Triggers
        null, // Project BIN Dir
        false, // Reuse Reports
        null, null, null, "UTF-8"
    );
  }

  /**
   * Asserts that the Sonar executed with given arguments.
   *
   * @param build build
   * @param args  command line arguments
   * @throws Exception if something is wrong
   */
  protected void assertSonarExecution(AbstractBuild build, String args) throws Exception {
    // Check command line arguments
    assertLogContains(args + " -e -B sonar:sonar", build);

    // Check that Sonar Plugin started
//    assertLogContains("[INFO] Sonar host: " + SONAR_HOST, build);

    // SONARPLUGINS-320: Check that small badge was added to build history
    assertNotNull(
        BuildSonarAction.class.getSimpleName() + " not found",
        build.getAction(BuildSonarAction.class)
    );

    // SONARPLUGINS-165: Check that link added to project
    // FIXME Godin: I don't know why, but this don't work for FreeStyleProject
    // AbstractProject project = build.getProject();
    // assertNotNull(project.getAction(ProjectSonarAction.class));
  }

  protected void assertNoSonarExecution(AbstractBuild build, String cause) throws Exception {
    assertLogContains(cause, build);
    // SONARPLUGINS-320: Check that small badge was not added to build history
    assertNull(
        BuildSonarAction.class.getSimpleName() + " found",
        build.getAction(BuildSonarAction.class)
    );
  }
}
