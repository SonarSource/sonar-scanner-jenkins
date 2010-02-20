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
        SONAR_HOST,
        null, null, null, null, // Database Properties
        "-P${VAR_PROFILE}", // Additinal Properties
        null
    ));

    FreeStyleProject project = createFreeStyleProject("FreeStyleProject");
    project.addProperty(new ParametersDefinitionProperty(
        new StringParameterDefinition("VAR_SUBDIR", "subdir"),
        new StringParameterDefinition("VAR_PROFILE", "release")
    ));
    String varPomName = "${VAR_SUBDIR}/pom.xml";
    String pomName = "subdir/pom.xml";
    project.getPublishersList().add(new SonarPublisher(
        SONAR_INSTALLATION_NAME,
        null,
        "-Ddir=${VAR_SUBDIR}",
        null, // Maven OPTS
        "default", // Maven Installation Name
        varPomName,
        PROJECT_CONFIG
    ));
    AbstractBuild build = build(project);

    // Check that POM generated
    // TODO validate POM
    assertTrue(build.getWorkspace().child(pomName).exists());
    assertLogContains("sonar:sonar", build);
    assertLogContains("-f " + pomName, build);
    assertLogContains("-Ddir=subdir", build);
    assertLogContains("-Prelease", build);
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
