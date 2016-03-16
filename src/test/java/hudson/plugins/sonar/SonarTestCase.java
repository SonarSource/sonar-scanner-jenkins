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

import hudson.plugins.sonar.action.SonarBuildBadgeAction;
import hudson.plugins.sonar.action.SonarProjectIconAction;
import hudson.tasks.Builder;
import hudson.Functions;
import hudson.maven.MavenModuleSet;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.Run;
import hudson.plugins.sonar.model.TriggersConfig;
import hudson.scm.NullSCM;
import hudson.tasks.Maven;
import hudson.util.jna.GNUCLibrary;
import jenkins.triggers.SCMTriggerItem;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.SingleFileSCM;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Evgeny Mandrikov
 */
public abstract class SonarTestCase {

  @Rule
  public JenkinsRule j = new JenkinsRule();

  /**
   * Setting this to non-existent host, allows us to avoid intersection with exist Sonar.
   */
  public static final String SONAR_HOST = "http://example.org:9999/sonar";
  public static final String DATABASE_PASSWORD = "password";

  public static final String ROOT_POM = "sonar-pom.xml";
  public static final String SONAR_INSTALLATION_NAME = "default";

  /**
   * Returns Fake Maven Installation.
   *
   * @return Fake Maven Installation
   * @throws Exception if something is wrong
   */
  protected Maven.MavenInstallation configureDefaultMaven() throws Exception {
    File mvn = new File(getClass().getResource("SonarTestCase/maven/bin/mvn").toURI().getPath());
    if (!Functions.isWindows()) {
      // noinspection OctalInteger
      GNUCLibrary.LIBC.chmod(mvn.getPath(), 0755);
    }
    String home = mvn.getParentFile().getParentFile().getAbsolutePath();
    Maven.MavenInstallation mavenInstallation = new Maven.MavenInstallation("default", home, JenkinsRule.NO_PROPERTIES);
    j.jenkins.getDescriptorByType(Maven.DescriptorImpl.class).setInstallations(mavenInstallation);
    return mavenInstallation;
  }

  protected SonarInstallation configureDefaultSonar() {
    return configureSonar(new SonarInstallation(SONAR_INSTALLATION_NAME, null, null, null, null, null, null, null, null, null, null, null, null));
  }
  
  protected SonarInstallation configureDefaultSonar(String version) {
    return configureSonar(new SonarInstallation(SONAR_INSTALLATION_NAME, null, version, null, null, null, null, null, null, null, null, null, null));
  }

  protected SonarInstallation configureSonar(SonarInstallation sonarInstallation) {
    j.jenkins.getDescriptorByType(SonarGlobalConfiguration.class).setInstallations(sonarInstallation);
    return sonarInstallation;
  }

  protected SonarRunnerInstallation configureDefaultSonarRunner(boolean broken) throws Exception {
    File exe = new File(getClass().getResource("SonarTestCase/sonar-runner" + (broken ? "-broken" : "") + "/bin/sonar-runner").toURI().getPath());
    if (!Functions.isWindows()) {
      // noinspection OctalInteger
      GNUCLibrary.LIBC.chmod(exe.getPath(), 0755);
    }
    String home = exe.getParentFile().getParentFile().getAbsolutePath();
    SonarRunnerInstallation runnerInstallation = new SonarRunnerInstallation("default", home, JenkinsRule.NO_PROPERTIES);
    j.jenkins.getDescriptorByType(SonarRunnerInstallation.DescriptorImpl.class).setInstallations(runnerInstallation);
    return runnerInstallation;
  }

  protected MavenModuleSet setupSonarMavenProject() throws Exception {
    return setupSonarMavenProject("pom.xml");
  }

  protected String getPom(AbstractBuild<?, ?> build, String pomName) {
    return build.getWorkspace().child(pomName).getRemote();
  }

  protected MavenModuleSet setupSonarMavenProject(String pomName) throws Exception {
    final MavenModuleSet project = j.jenkins.createProject(MavenModuleSet.class, "MavenProject");
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

  protected FreeStyleProject setupFreeStyleProjectWithSonarRunner() throws Exception {
    return setupFreeStyleProject(new SonarRunnerBuilder(null, null, null, null, null, null, null, null));
  }

  protected FreeStyleProject setupFreeStyleProject(Builder b) throws Exception {
    FreeStyleProject project = j.createFreeStyleProject("FreeStyleProject");
    // Setup SCM
    project.setScm(new NullSCM());
    // Setup SonarQube step
    project.getBuildersList().add(b);
    return project;
  }

  protected Run<?, ?> build(Job<?, ?> project) throws Exception {
    return build(project, null);
  }

  protected Run<?, ?> build(Job<?, ?> project, Result expectedStatus) throws Exception {
    return build(project, new TriggersConfig.SonarCause(), expectedStatus);
  }

  protected Run<?, ?> build(Job<?, ?> project, Cause cause, Result expectedStatus) throws Exception {
    Run<?, ?> build = null;
    if (project instanceof AbstractProject) {
      build = ((AbstractProject<?, ?>) project).scheduleBuild2(0, cause).get();
    } else if (project instanceof SCMTriggerItem) {
      build = (Run<?, ?>) ((SCMTriggerItem) project).scheduleBuild2(0, new CauseAction(cause)).get();
    }
    if (expectedStatus != null && build != null) {
      j.assertBuildStatus(expectedStatus, build);
    }
    return build;
  }

  protected static SonarPublisher newSonarPublisherForMavenProject() {
    return new SonarPublisher(SONAR_INSTALLATION_NAME, null, null, null, null, null, null, null, null, null, false);
  }

  protected static SonarPublisher newSonarPublisherForFreeStyleProject(String pomName) {
    return new SonarPublisher(
      SONAR_INSTALLATION_NAME,
      null,
      new TriggersConfig(),
      null,
      null,
      "default", // Maven Installation Name
      pomName, // Root POM
      null,
      null,
      null,
      false);
  }

  /**
   * Asserts that Sonar executed with given arguments.
   *
   * @param build build
   * @param args command line arguments
   * @throws Exception if something is wrong
   */
  protected void assertSonarExecution(Run<?, ?> build, String args, boolean success) throws Exception {
    // Check command line arguments
    assertLogContains(args, build);

    if (success) {
      // SONARPLUGINS-320: Check that small badge was added to build history
      assertThat(build.getAction(SonarBuildBadgeAction.class)).as(SonarBuildBadgeAction.class.getSimpleName() + " not found").isNotNull();
    } else {
      // SONARJNKNS-203 Do not add link if build has failed
      assertThat(build.getAction(SonarBuildBadgeAction.class)).as(SonarBuildBadgeAction.class.getSimpleName() + " not found").isNotNull();
      assertThat(build.getAction(SonarBuildBadgeAction.class).getUrl()).isNull();
    }
    // SONARPLUGINS-165: Check that link added to project
    Job<?, ?> parent = build.getParent();
    if (parent instanceof AbstractProject) {
      assertThat(((AbstractProject<?, ?>) parent).getAction(SonarProjectIconAction.class)).isNotNull();
    }
  }

  protected void assertSonarExecution(Run<?, ?> build, boolean success) throws Exception {
    assertSonarExecution(build, "", success);
  }

  protected void assertNoSonarExecution(Run<?, ?> build, String cause) throws Exception {
    assertLogContains(cause, build);
    // SONARPLUGINS-320: Check that small badge was not added to build history
    assertThat(build.getAction(SonarBuildBadgeAction.class)).as(SonarBuildBadgeAction.class.getSimpleName() + " found").isNull();
  }

  protected void assertLogContains(String substring, Run<?, ?> run) throws IOException {
    String log = JenkinsRule.getLog(run);
    assertThat(log).contains(substring);
  }

  /**
   * Asserts that the console output of the build doesn't contains the given substring.
   *
   * @param substring substring to check
   * @param run run to check
   * @throws Exception if something wrong
   */
  protected void assertLogDoesntContains(String substring, Run<?, ?> run) throws Exception {
    String log = JenkinsRule.getLog(run);
    assertThat(log).doesNotContain(substring);
  }
}
