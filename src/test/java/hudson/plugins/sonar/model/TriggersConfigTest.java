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
import hudson.triggers.SCMTrigger;
import hudson.triggers.TimerTrigger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TriggersConfigTest {

  private static final Cause UPSTREAM_CAUSE = mock(Cause.UpstreamCause.class);
  private static final Cause SCM_CAUSE = mock(SCMTrigger.SCMTriggerCause.class);
  private static final Cause TIMER_CAUSE = mock(TimerTrigger.TimerTriggerCause.class);

  private TriggersConfig triggers;

  @Before
  public void setUp() {
    triggers = new TriggersConfig();
  }

  @Test
  public void our_internal_cause() {
    AbstractBuild build = mockBuildWithCauses(new TriggersConfig.SonarCause());
    assertThat(triggers.isSkipSonar(build), nullValue());
  }

  @Test
  public void skip_if_build_fails() {
    AbstractBuild build = mockBuildWithCauses(new TriggersConfig.SonarCause());
    when(build.getResult()).thenReturn(null, Result.SUCCESS, Result.UNSTABLE, Result.FAILURE, Result.NOT_BUILT, Result.ABORTED);
    assertThat(triggers.isSkipSonar(build), nullValue());
    assertThat(triggers.isSkipSonar(build), nullValue());
    assertThat(triggers.isSkipSonar(build), nullValue());
    assertThat(triggers.isSkipSonar(build), not(nullValue()));
    assertThat(triggers.isSkipSonar(build), not(nullValue()));
    assertThat(triggers.isSkipSonar(build), not(nullValue()));
  }

  @Test
  public void timer_cause() {
    AbstractBuild build = mockBuildWithCauses(TIMER_CAUSE);
    assertThat(triggers.isSkipSonar(build), nullValue());
  }

  @Test
  public void scm_change_cause() {
    AbstractBuild build = mockBuildWithCauses(SCM_CAUSE);
    assertThat(triggers.isSkipSonar(build), nullValue());
    triggers.setSkipScmCause(true);
    assertThat(triggers.isSkipSonar(build), not(nullValue()));
  }

  @Test
  public void upstream_cause() {
    AbstractBuild build = mockBuildWithCauses(UPSTREAM_CAUSE);
    assertThat(triggers.isSkipSonar(build), nullValue());
    triggers.setSkipUpstreamCause(true);
    assertThat(triggers.isSkipSonar(build), not(nullValue()));
  }

  /**
   * See SONARPLUGINS-378
   */
  @Ignore("Ignored due to changed behavior - black list strategy")
  @Test
  public void custom_cause() {
    AbstractBuild build = mockBuildWithCauses(new CustomCause());
    assertThat(triggers.isSkipSonar(build), not(nullValue()));
  }

  @Test
  public void multiple_causes() {
    triggers.setSkipScmCause(true);
    triggers.setSkipUpstreamCause(true);
    AbstractBuild build = mockBuildWithCauses(SCM_CAUSE, TIMER_CAUSE);
    assertThat(triggers.isSkipSonar(build), nullValue());

    build = mockBuildWithCauses(SCM_CAUSE, UPSTREAM_CAUSE);
    assertThat(triggers.isSkipSonar(build), not(nullValue()));
    triggers.setSkipScmCause(false);
    assertThat(triggers.isSkipSonar(build), nullValue());
  }

  /**
   * See SONARPLUGINS-1338
   */
  @Test
  public void env_var() {
    AbstractBuild build = mockBuildWithCauses(new TriggersConfig.SonarCause());
    Map<String, String> vars = new HashMap<String, String>();
    when(build.getBuildVariables()).thenReturn(vars);
    assertThat(triggers.isSkipSonar(build), nullValue());
    triggers.setEnvVar("SKIP_SONAR");
    assertThat(triggers.isSkipSonar(build), nullValue());
    vars.put("SKIP_SONAR", "true");
    assertThat(triggers.isSkipSonar(build), not(nullValue()));
  }

  private static class CustomCause extends Cause {
    @Override
    public String getShortDescription() {
      return null;
    }
  }

  private static AbstractBuild mockBuildWithCauses(Cause... causes) {
    AbstractBuild build = mock(AbstractBuild.class);
    when(build.getCauses()).thenReturn(Arrays.asList(causes));
    return build;
  }
}
