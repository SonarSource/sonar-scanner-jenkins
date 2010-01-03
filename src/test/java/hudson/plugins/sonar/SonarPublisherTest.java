/*
 * Sonar, entreprise quality control tool.
 * Copyright (C) 2007-2008 Hortis-GRC SA
 * mailto:be_agile HAT hortis DOT ch
 *
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

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.*;
import hudson.util.ArgumentListBuilder;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Evgeny Mandrikov
 */
public class SonarPublisherTest {
  private SonarPublisher newInstance() {
    return newInstance(false, false, false, false, false);
  }

  private SonarPublisher newInstance(
      boolean snapshotDependencyBuilds,
      boolean scmBuilds,
      boolean timerBuilds,
      boolean userBuilds,
      boolean skipIfBuildFails
  ) {
    return new SonarPublisher(
        "test",
        "",
        false,
        null, null, null, null, null, null, null, null, null,
        snapshotDependencyBuilds,
        scmBuilds,
        timerBuilds,
        userBuilds,
        skipIfBuildFails,
        null, false, null, null, null, null
    );
  }

  @Test
  public void testIsSkipSonar() {
    SonarInstallation sonarInstallation = mock(SonarInstallation.class);
    SonarPublisher publisher = spy(newInstance(false, false, false, false, true));
    AbstractBuild build = mock(AbstractBuild.class);

    // Skip If Build Fails
    when(
        publisher.isSkipIfBuildFails()
    ).thenReturn(true);

    when(
        build.getResult()
    ).thenReturn(Result.ABORTED);
    assertNotNull(publisher.isSkipSonar(build, sonarInstallation));

    when(
        build.getResult()
    ).thenReturn(Result.NOT_BUILT);
    assertNotNull(publisher.isSkipSonar(build, sonarInstallation));

    when(
        build.getResult()
    ).thenReturn(Result.UNSTABLE);
    assertNotNull(publisher.isSkipSonar(build, sonarInstallation));

    when(
        build.getResult()
    ).thenReturn(Result.SUCCESS);

    // Skip If User Build
    CauseAction action = new CauseAction(new Cause.UserCause());
    when(
        build.getAction(CauseAction.class)
    ).thenReturn(action);

    when(
        publisher.isUserBuilds()
    ).thenReturn(true);
    assertNull(publisher.isSkipSonar(build, sonarInstallation));

    when(
        publisher.isUserBuilds()
    ).thenReturn(false);
    assertNotNull(publisher.isSkipSonar(build, sonarInstallation));

    // TODO Skip If Timer Build

    // TODO Skip If Scm Build

    // TODO Skip If Snapshot Dependency Build

  }

  @Test
  public void testBuildCommand() throws Exception {
    SonarInstallation sonarInstallation = mock(SonarInstallation.class);
    Launcher launcher = mock(Launcher.class);
    AbstractBuild build = mock(AbstractBuild.class);

    when(
        launcher.isUnix()
    ).thenReturn(true);
    when(
        build.getEnvironment(any(TaskListener.class))
    ).thenReturn(new EnvVars());

    SonarPublisher publisher = newInstance();

    // see bug SONARPLUGINS-263 (pom with spaces)
    ArgumentListBuilder args = publisher.buildCommand(
        launcher,
        null,
        build,
        sonarInstallation,
        "mvn",
        "space test/pom.xml",
        null
    );

    assertEquals(
        "mvn -e -B -f \"space test/pom.xml\" sonar:sonar",
        args.toStringWithQuote()
    );
  }

}
