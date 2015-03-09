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
package hudson.plugins.sonar.model;

import hudson.EnvVars;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.triggers.SCMTrigger;
import hudson.triggers.TimerTrigger;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TriggersConfigTest {

  private static final Cause UPSTREAM_CAUSE = mock(Cause.UpstreamCause.class);
  private static final Cause SCM_CAUSE = mock(SCMTrigger.SCMTriggerCause.class);
  private static final Cause TIMER_CAUSE = mock(TimerTrigger.TimerTriggerCause.class);

  private TriggersConfig triggers;
  private BuildListener listener;

  @Before
  public void setUp() {
    triggers = new TriggersConfig();
    listener = mock(BuildListener.class);
  }

  @Test
  public void our_internal_cause() throws IOException, InterruptedException {
    AbstractBuild<?, ?> build = mockBuildWithCauses(new TriggersConfig.SonarCause());
    assertThat(triggers.isSkipSonar(build, listener)).isNull();
  }

  @Test
  public void skip_if_build_fails() throws IOException, InterruptedException {
    AbstractBuild<?, ?> build = mockBuildWithCauses(new TriggersConfig.SonarCause());
    when(build.getResult()).thenReturn(null, Result.SUCCESS, Result.UNSTABLE, Result.FAILURE, Result.NOT_BUILT, Result.ABORTED);
    assertThat(triggers.isSkipSonar(build, listener)).isNull();
    assertThat(triggers.isSkipSonar(build, listener)).isNull();
    assertThat(triggers.isSkipSonar(build, listener)).isNull();
    assertThat(triggers.isSkipSonar(build, listener)).isNotNull();
    assertThat(triggers.isSkipSonar(build, listener)).isNotNull();
    assertThat(triggers.isSkipSonar(build, listener)).isNotNull();
  }

  @Test
  public void timer_cause() throws IOException, InterruptedException {
    AbstractBuild<?, ?> build = mockBuildWithCauses(TIMER_CAUSE);
    assertThat(triggers.isSkipSonar(build, listener)).isNull();
  }

  @Test
  public void scm_change_cause() throws IOException, InterruptedException {
    AbstractBuild<?, ?> build = mockBuildWithCauses(SCM_CAUSE);
    assertThat(triggers.isSkipSonar(build, listener)).isNull();
    triggers.setSkipScmCause(true);
    assertThat(triggers.isSkipSonar(build, listener)).isNotNull();
  }

  @Test
  public void upstream_cause() throws IOException, InterruptedException {
    AbstractBuild<?, ?> build = mockBuildWithCauses(UPSTREAM_CAUSE);
    assertThat(triggers.isSkipSonar(build, listener)).isNull();
    triggers.setSkipUpstreamCause(true);
    assertThat(triggers.isSkipSonar(build, listener)).isNotNull();
  }

  @Test
  public void multiple_causes() throws IOException, InterruptedException {
    triggers.setSkipScmCause(true);
    triggers.setSkipUpstreamCause(true);
    AbstractBuild<?, ?> build = mockBuildWithCauses(SCM_CAUSE, TIMER_CAUSE);
    assertThat(triggers.isSkipSonar(build, listener)).isNull();

    build = mockBuildWithCauses(SCM_CAUSE, UPSTREAM_CAUSE);
    assertThat(triggers.isSkipSonar(build, listener)).isNotNull();
    triggers.setSkipScmCause(false);
    assertThat(triggers.isSkipSonar(build, listener)).isNull();
  }

  /**
   * See SONARPLUGINS-1338
   */
  @Test
  public void build_parameters() throws IOException, InterruptedException {
    AbstractBuild<?, ?> build = mockBuildWithCauses(new TriggersConfig.SonarCause());
    EnvVars env_vars = new EnvVars();
    when(build.getEnvironment(listener)).thenReturn(env_vars);
    Map<String, String> build_vars = new HashMap<String, String>();
    when(build.getBuildVariables()).thenReturn(build_vars);
    assertThat(triggers.isSkipSonar(build, listener)).isNull();
    triggers.setEnvVar("SKIP_SONAR");
    assertThat(triggers.isSkipSonar(build, listener)).isNull();
    build_vars.put("SKIP_SONAR", "true");
    assertThat(triggers.isSkipSonar(build, listener)).isNotNull();
  }

  /**
   * See SONARPLUGINS-1886
   */
  @Test
  public void env_var() throws IOException, InterruptedException {
    AbstractBuild<?, ?> build = mockBuildWithCauses(new TriggersConfig.SonarCause());
    EnvVars env_vars = new EnvVars();
    when(build.getEnvironment(listener)).thenReturn(env_vars);
    Map<String, String> build_vars = new HashMap<String, String>();
    when(build.getBuildVariables()).thenReturn(build_vars);
    assertThat(triggers.isSkipSonar(build, listener)).isNull();
    triggers.setEnvVar("SKIP_SONAR");
    assertThat(triggers.isSkipSonar(build, listener)).isNull();
    env_vars.put("SKIP_SONAR", "true");
    assertThat(triggers.isSkipSonar(build, listener)).isNotNull();
  }

  private static AbstractBuild<?, ?> mockBuildWithCauses(Cause... causes) {
    AbstractBuild<?, ?> build = mock(AbstractBuild.class);
    when(build.getCauses()).thenReturn(Arrays.asList(causes));
    return build;
  }
}
