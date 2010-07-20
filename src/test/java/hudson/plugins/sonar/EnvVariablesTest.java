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
        SONAR_HOST, null,
        null, null, null, null, // Database Properties
        null, "-P${VAR_PROFILE}", // Additinal Properties
        null
    ));

    FreeStyleProject project = createFreeStyleProject("FreeStyleProject");
    project.addProperty(new ParametersDefinitionProperty(
        new StringParameterDefinition("VAR_SUBDIR", "subdir"),
        new StringParameterDefinition("VAR_PROFILE", "release")
    ));
//    String varPomName = "${VAR_SUBDIR}/pom.xml";
    String pomName = "subdir/pom.xml";
    project.getPublishersList().add(new SonarPublisher(
        SONAR_INSTALLATION_NAME,
        null,
        "-Ddir=${VAR_SUBDIR}",
        null, // Maven OPTS
        "default", // Maven Installation Name
//        varPomName,
        pomName,
        PROJECT_CONFIG
    ));
    AbstractBuild build = build(project);

    // Check that POM generated
    // TODO validate POM
    assertTrue(build.getWorkspace().child(pomName).exists());
    assertLogContains("sonar:sonar", build);
    assertLogContains("-f " + getPom(build, pomName), build);
    assertLogContains("-Ddir=subdir", build);
    assertLogContains("-Prelease", build);
  }
}
