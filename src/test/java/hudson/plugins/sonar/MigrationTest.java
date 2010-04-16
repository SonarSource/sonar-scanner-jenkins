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

import hudson.model.AbstractProject;
import hudson.plugins.sonar.model.LightProjectConfig;
import hudson.plugins.sonar.model.ReportsConfig;
import org.junit.Ignore;
import org.jvnet.hudson.test.recipes.LocalData;

/**
 * @author Evgeny Mandrikov
 */
@Ignore("Due to http://jira.codehaus.org/browse/SONARPLUGINS-402")
public class MigrationTest extends SonarTestCase {
  @LocalData
  public void testOldMaven() {
    SonarPublisher sonarPublisher = getSonarPublisher(getProject("oldMaven"));

    verifyGeneral(sonarPublisher);
    assertFalse(sonarPublisher.isUseSonarLight());
  }

  @LocalData
  public void testOldFreeStyle() {
    SonarPublisher sonarPublisher = getSonarPublisher(getProject("oldFreeStyle"));

    verifyGeneralFreeStyle(sonarPublisher);
    assertFalse(sonarPublisher.isUseSonarLight());
  }

  @LocalData
  public void testOldLight() {
    SonarPublisher sonarPublisher = getSonarPublisher(getProject("oldLight"));

    verifyGeneralFreeStyle(sonarPublisher);
    verifyLight(sonarPublisher);
    assertFalse(sonarPublisher.getLightProject().isReuseReports());
  }

  @LocalData
  public void testOldLightReuseReports() {
    SonarPublisher sonarPublisher = getSonarPublisher(getProject("oldLightReuseReports"));

    verifyGeneralFreeStyle(sonarPublisher);
    verifyLight(sonarPublisher);

    LightProjectConfig project = sonarPublisher.getLightProject();
    assertTrue(project.isReuseReports());
    ReportsConfig reports = project.getReports();
    assertEquals("target/surefire-reports", reports.getSurefireReportsPath());
    assertEquals("target/site/cobertura/coverage.xml", reports.getCoberturaReportPath());
    assertEquals("target/site/clover/clover.xml", reports.getCloverReportPath());
  }

  // ====================================================

  private void verifyGeneralFreeStyle(SonarPublisher sonarPublisher) {
    verifyGeneral(sonarPublisher);
    assertEquals("My Maven", sonarPublisher.getMavenInstallationName());
  }

  private void verifyGeneral(SonarPublisher sonarPublisher) {
    assertEquals((Integer) 1, sonarPublisher.getConfigVersion());
    assertEquals("My Sonar", sonarPublisher.getInstallationName());
    assertEquals("-Dproperty=value", sonarPublisher.getJobAdditionalProperties());
    assertEquals("-Xmx512m", sonarPublisher.getMavenOpts());

    verifyTriggers(sonarPublisher);
  }

  private void verifyLight(SonarPublisher sonarPublisher) {
    assertTrue(sonarPublisher.isUseSonarLight());

    LightProjectConfig project = sonarPublisher.getLightProject();
    assertEquals("example.org", project.getGroupId());
    assertEquals("myproject", project.getArtifactId());
    assertEquals("My Project", project.getProjectName());

    assertEquals("0.1-SNAPSHOT", project.getProjectVersion());
    assertEquals("Project description.", project.getProjectDescription());

    assertEquals("1.5", project.getJavaVersion());
    assertEquals("src/java", project.getProjectSrcDir());
    assertEquals("UTF-8", project.getProjectSrcEncoding());
    assertEquals("target/classes", project.getProjectBinDir());
  }

  private void verifyTriggers(SonarPublisher sonarPublisher) {
    assertFalse("Use global triggers", sonarPublisher.isUseGlobalTriggers());
    assertTrue(sonarPublisher.getTriggers().isScmBuilds());
    assertTrue(sonarPublisher.getTriggers().isTimerBuilds());
    assertTrue(sonarPublisher.getTriggers().isSnapshotDependencyBuilds());
    assertTrue(sonarPublisher.getTriggers().isSkipIfBuildFails());
    assertTrue(
        "triggers.userBuilds added in 1.2 and should be true after migration",
        sonarPublisher.getTriggers().isUserBuilds()
    );
  }

  private AbstractProject getProject(String name) {
    return (AbstractProject) hudson.getItem(name);
  }

  private SonarPublisher getSonarPublisher(AbstractProject project) {
    return (SonarPublisher) project.getPublishersList().get(SonarPublisher.class);
  }
}
