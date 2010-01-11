package hudson.plugins.sonar;

import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterDefinition;
import org.jvnet.hudson.test.Bug;

/**
 * SONARPLUGINS-305:
 * Environment variable is not being used by hudson-sonar-plugin.
 *
 * @author Evgeny Mandrikov
 */
@Bug(305)
public class EnvVariablesTest extends SonarTestCase {
  /**
   * TODO add more variables
   *
   * @throws Exception if something is wrong
   */
  public void testFreeStyleProject() throws Exception {
    configureDefaultMaven();
    configureSonar(new SonarInstallation(
        SONAR_INSTALLATION_NAME,
        false,
        "${VAR_SONAR_HOST}",
        null, null, null, DATABASE_PASSWORD, // Database Properties
        "-P${VAR_PROFILE}" // Additinal Properties
    ));

    FreeStyleProject project = createFreeStyleProject("FreeStyleProject");
    project.addProperty(new ParametersDefinitionProperty(
        new StringParameterDefinition("VAR_SUBDIR", "subdir"),
        new StringParameterDefinition("VAR_SONAR_HOST", SONAR_HOST),
        new StringParameterDefinition("VAR_PROFILE", "release")
    ));
    String varPomName = "${VAR_SUBDIR}/pom.xml";
    String pomName = "subdir/pom.xml";
    project.getPublishersList().add(new SonarPublisher(
        SONAR_INSTALLATION_NAME,
        "-Ddir=${VAR_SUBDIR}",
        true, // Sonar Light
        "test", "test",
        "", // TODO can be ${JOB_NAME}
        "0.1-SNAPSHOT", // Project Information
        "src", // Project SRC Dir
        null, // Java Version
        "Test project", // Project Description
        null, // Maven OPTS
        "default", // Maven Installation Name
        varPomName, // Root POM
        false, false, false, false, true, // Triggers
        null, // Project BIN Dir
        false, // Reuse Reports
        null, null, null, "UTF-8"
    ));
    AbstractBuild build = build(project);

    // Check that POM generated
    // TODO validate POM
    assertTrue(build.getWorkspace().child(pomName).exists());
    assertSonarExecution(build, "-f " + pomName +
        " -DVAR_SUBDIR=subdir" +
        " -DVAR_SONAR_HOST=" + SONAR_HOST +
        " -DVAR_PROFILE=release" +
        " -Dsonar.jdbc.password=" + DATABASE_PASSWORD +
        " -Dsonar.host.url=" + SONAR_HOST +
        " -Prelease" +
        " -Ddir=subdir"
    );
  }

  /* TODO Godin: WTF? original build failed
  public void testMavenProject() throws Exception {
    configureDefaultMaven();
    configureDefaultSonar();

    String pomName = "subdir/pom.xml";
    MavenModuleSet project = setupMavenProject(pomName);
    project.addProperty(new ParametersDefinitionProperty(
        new StringParameterDefinition("VAR_SUBDIR", "subdir")
    ));
    project.setRootPOM("${VAR_SUBDIR}/pom.xml");
    MavenModuleSetBuild build = build(project);

    System.out.println(build.getLog());
  }
  */
}
