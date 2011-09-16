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
package hudson.plugins.sonar.model;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.triggers.SCMTrigger;
import hudson.triggers.TimerTrigger;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

public class TriggersConfigTest {

  private TriggersConfig triggers;

  @Before
  public void setUp() {
    triggers = new TriggersConfig();
  }

  @Test
  public void timer_cause() {
    AbstractBuild build = mockBuildWithCauses(new TimerTrigger.TimerTriggerCause());
    assertThat(triggers.isSkipSonar(build), not(nullValue()));
    triggers.setTimerBuilds(true);
    assertThat(triggers.isSkipSonar(build), nullValue());
  }

  @Test
  public void scm_change_cause() {
    AbstractBuild build = mockBuildWithCauses(new SCMTrigger.SCMTriggerCause());
    assertThat(triggers.isSkipSonar(build), not(nullValue()));
    triggers.setScmBuilds(true);
    assertThat(triggers.isSkipSonar(build), nullValue());
  }

  @Test
  public void upstream_cause() {
    AbstractBuild build = mockBuildWithCauses(mock(Cause.UpstreamCause.class));
    assertThat(triggers.isSkipSonar(build), not(nullValue()));
    triggers.setSnapshotDependencyBuilds(true);
    assertThat(triggers.isSkipSonar(build), nullValue());
  }

  @Test
  public void our_internal_cause() {
    AbstractBuild build = mockBuildWithCauses(new TriggersConfig.SonarCause());
    assertThat(triggers.isSkipSonar(build), nullValue());
  }

  /**
   * See SONARPLUGINS-216
   */
  @Test
  public void user_cause() {
    AbstractBuild build = mockBuildWithCauses(new Cause.UserCause());
    assertThat(triggers.isSkipSonar(build), not(nullValue()));
    triggers.setUserBuilds(true);
    assertThat(triggers.isSkipSonar(build), nullValue());
  }

  /**
   * See SONARPLUGINS-461
   */
  @Test
  public void skip_if_build_fails() {
    triggers.setUserBuilds(true);
    triggers.setSkipIfBuildFails(true);
    AbstractBuild build = mockBuildWithCauses(new Cause.UserCause());
    when(build.getResult()).thenReturn(null, Result.SUCCESS, Result.UNSTABLE, Result.FAILURE, Result.NOT_BUILT, Result.ABORTED);
    assertThat(triggers.isSkipSonar(build), nullValue());
    assertThat(triggers.isSkipSonar(build), nullValue());
    assertThat(triggers.isSkipSonar(build), nullValue());
    assertThat(triggers.isSkipSonar(build), not(nullValue()));
    assertThat(triggers.isSkipSonar(build), not(nullValue()));
    assertThat(triggers.isSkipSonar(build), not(nullValue()));
  }

  /**
   * See SONARPLUGINS-461
   */
  @Test
  public void do_not_skip_if_build_fails() {
    triggers.setUserBuilds(true);
    AbstractBuild build = mockBuildWithCauses(new Cause.UserCause());
    when(build.getResult()).thenReturn(null, Result.SUCCESS, Result.UNSTABLE, Result.FAILURE, Result.NOT_BUILT, Result.ABORTED);
    assertThat(triggers.isSkipSonar(build), nullValue());
    assertThat(triggers.isSkipSonar(build), nullValue());
    assertThat(triggers.isSkipSonar(build), nullValue());
    assertThat(triggers.isSkipSonar(build), nullValue());
    assertThat(triggers.isSkipSonar(build), not(nullValue()));
    assertThat(triggers.isSkipSonar(build), not(nullValue()));
  }

  /**
   * See SONARPLUGINS-378
   */
  @Test
  public void custom_cause() {
    AbstractBuild build = mockBuildWithCauses(new CustomCause());
    assertThat(triggers.isSkipSonar(build), not(nullValue()));
  }

  /**
   * See SONARPLUGINS-973.
   * Given: Sonar configured to be launched by timer, build was caused due to both SCM change and timer.
   * Expected: Sonar should be executed.
   */
  @Test
  public void multiple_causes() {
    triggers.setTimerBuilds(true);
    AbstractBuild build = mockBuildWithCauses(
        new SCMTrigger.SCMTriggerCause(),
        new TimerTrigger.TimerTriggerCause());
    assertThat(triggers.isSkipSonar(build), nullValue());
  }

  private static class CustomCause extends Cause {
    @Override
    public String getShortDescription() {
      return null;
    }
  }

  private static AbstractBuild mockBuildWithCauses(Cause... causes) {
    CauseAction causeAction = mock(CauseAction.class);
    when(causeAction.getCauses()).thenReturn(Arrays.asList(causes));
    AbstractBuild build = mock(AbstractBuild.class);
    when(build.getAction(CauseAction.class)).thenReturn(causeAction);
    return build;
  }
}
