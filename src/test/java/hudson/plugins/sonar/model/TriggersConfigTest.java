/*
 * SonarQube Scanner for Jenkins
 * Copyright (C) 2007-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package hudson.plugins.sonar.model;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.Result;
import hudson.triggers.SCMTrigger;
import hudson.triggers.TimerTrigger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TriggersConfigTest {

  private static final Cause UPSTREAM_CAUSE = mock(Cause.UpstreamCause.class);
  private static final Cause SCM_CAUSE = mock(SCMTrigger.SCMTriggerCause.class);
  private static final Cause TIMER_CAUSE = mock(TimerTrigger.TimerTriggerCause.class);

  private TriggersConfig triggers;
  private BuildListener listener;

  @BeforeEach
  void setUp() {
    triggers = new TriggersConfig();
    listener = mock(BuildListener.class);
  }

  @Test
  void our_internal_cause() throws Exception {
    AbstractBuild<?, ?> build = mockBuildWithCauses(new TriggersConfig.SonarCause());
    assertThat(triggers.isSkipSonar(build, listener)).isNull();
  }

  @Test
  void skip_if_build_fails() throws Exception {
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
  void timer_cause() throws Exception {
    AbstractBuild<?, ?> build = mockBuildWithCauses(TIMER_CAUSE);
    assertThat(triggers.isSkipSonar(build, listener)).isNull();
  }

  @Test
  void scm_change_cause() throws Exception {
    AbstractBuild<?, ?> build = mockBuildWithCauses(SCM_CAUSE);
    assertThat(triggers.isSkipSonar(build, listener)).isNull();
    triggers.setSkipScmCause(true);
    assertThat(triggers.isSkipSonar(build, listener)).isNotNull();
  }

  @Test
  void upstream_cause() throws Exception {
    AbstractBuild<?, ?> build = mockBuildWithCauses(UPSTREAM_CAUSE);
    assertThat(triggers.isSkipSonar(build, listener)).isNull();
    triggers.setSkipUpstreamCause(true);
    assertThat(triggers.isSkipSonar(build, listener)).isNotNull();
  }

  @Test
  void multiple_causes() throws Exception {
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
  void build_parameters() throws Exception {
    AbstractBuild<?, ?> build = mockBuildWithCauses(new TriggersConfig.SonarCause());
    EnvVars env_vars = new EnvVars();
    when(build.getEnvironment(listener)).thenReturn(env_vars);
    Map<String, String> buildVars = new HashMap<>();
    when(build.getBuildVariables()).thenReturn(buildVars);
    when(build.getBuildVariableResolver()).thenCallRealMethod();
    assertThat(triggers.isSkipSonar(build, listener)).isNull();
    triggers.setEnvVar("SKIP_SONAR");
    assertThat(triggers.isSkipSonar(build, listener)).isNull();
    buildVars.put("SKIP_SONAR", "true");
    assertThat(triggers.isSkipSonar(build, listener)).isNotNull();
  }

  /**
   * See SONARPLUGINS-1886
   */
  @Test
  void env_var() throws Exception {
    AbstractBuild<?, ?> build = mockBuildWithCauses(new TriggersConfig.SonarCause());
    EnvVars env_vars = new EnvVars();
    when(build.getEnvironment(listener)).thenReturn(env_vars);
    Map<String, String> buildVars = new HashMap<>();
    when(build.getBuildVariables()).thenReturn(buildVars);
    when(build.getBuildVariableResolver()).thenCallRealMethod();
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
