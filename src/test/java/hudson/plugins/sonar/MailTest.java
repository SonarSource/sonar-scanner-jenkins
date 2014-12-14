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
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.Mailer;
import jenkins.model.JenkinsLocationConfiguration;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.mock_javamail.Mailbox;

import static org.fest.assertions.Assertions.assertThat;

/**
 * SONARPLUGINS-286:
 * If your build is successful and the post-build fails,
 * then the job will show as fail but you will not get an email.
 * <p>
 * TODO check that internal build was succesfull
 * </p>
 *
 * @author Evgeny Mandrikov
 */
@Issue("SONARPLUGINS-286")
public class MailTest extends SonarTestCase {
  private Mailbox inbox;
  private Mailer mailer;

  @Before
  public void setUp() throws Exception {
    configureDefaultMaven();
    configureDefaultSonar();
    // Configure Mailer and Mailbox
    JenkinsLocationConfiguration.get().setAdminAddress("admin@example.org");
    String recipient = "me@example.org";
    inbox = Mailbox.get(recipient);
    mailer = new Mailer("me@example.org", false, false);
  }

  @Ignore("Ingored due to changes in triggers")
  public void ignore_testMavenProject() throws Exception {
    MavenModuleSet project = setupMavenProject();
    project.getPublishersList().add(mailer);
    inbox.clear();
    AbstractBuild<?, ?> build = build(project, Result.FAILURE);

    assertSonarExecution(build, "-f " + getPom(build, "pom.xml"));
    assertThat(inbox.size()).isEqualTo(1);
  }

  @Test
  public void testFreeStyleProject() throws Exception {
    FreeStyleProject project = setupFreeStyleProject();
    project.getPublishersList().add(mailer);
    inbox.clear();
    AbstractBuild<?, ?> build = build(project, Result.FAILURE);

    assertSonarExecution(build, "-f " + getPom(build, "sonar-pom.xml"));
    assertThat(inbox.size()).isEqualTo(1);
  }
}
