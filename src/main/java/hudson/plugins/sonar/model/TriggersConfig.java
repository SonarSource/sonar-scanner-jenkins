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

import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Result;
import hudson.plugins.sonar.Messages;
import hudson.triggers.SCMTrigger;
import hudson.triggers.TimerTrigger;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @author Evgeny Mandrikov
 * @since 1.2
 */
public class TriggersConfig implements Serializable {
  private boolean scmBuilds;

  private boolean timerBuilds;

  /**
   * @since 1.2
   */
  private boolean userBuilds;

  private boolean snapshotDependencyBuilds;

  private boolean skipIfBuildFails;

  public TriggersConfig() {
  }

  @DataBoundConstructor
  public TriggersConfig(boolean scmBuilds, boolean timerBuilds, boolean userBuilds, boolean snapshotDependencyBuilds, boolean skipIfBuildFails) {
    this.scmBuilds = scmBuilds;
    this.timerBuilds = timerBuilds;
    this.userBuilds = userBuilds;
    this.snapshotDependencyBuilds = snapshotDependencyBuilds;
    this.skipIfBuildFails = skipIfBuildFails;
  }

  public boolean isScmBuilds() {
    return scmBuilds;
  }

  public void setScmBuilds(boolean scmBuilds) {
    this.scmBuilds = scmBuilds;
  }

  public boolean isTimerBuilds() {
    return timerBuilds;
  }

  public void setTimerBuilds(boolean timerBuilds) {
    this.timerBuilds = timerBuilds;
  }

  public boolean isUserBuilds() {
    return userBuilds;
  }

  public void setUserBuilds(boolean userBuilds) {
    this.userBuilds = userBuilds;
  }

  public boolean isSnapshotDependencyBuilds() {
    return snapshotDependencyBuilds;
  }

  public void setSnapshotDependencyBuilds(boolean snapshotDependencyBuilds) {
    this.snapshotDependencyBuilds = snapshotDependencyBuilds;
  }

  public boolean isSkipIfBuildFails() {
    return skipIfBuildFails;
  }

  public void setSkipIfBuildFails(boolean skipIfBuildFails) {
    this.skipIfBuildFails = skipIfBuildFails;
  }

  public String isSkipSonar(AbstractBuild<?, ?> build) {
    if (isSkipIfBuildFails() && build.getResult().isWorseThan(Result.UNSTABLE)) {
      return Messages.SonarPublisher_BadBuildStatus(build.getResult().toString());
    } else if (build.getResult().isWorseThan(Result.FAILURE)) {
      return Messages.SonarPublisher_BadBuildStatus(build.getResult().toString());
    } else if (!isScmBuilds() && isTrigger(build, SCMTrigger.SCMTriggerCause.class)) {
      return Messages.SonarPublisher_SCMBuild();
    } else if (!isTimerBuilds() && isTrigger(build, TimerTrigger.TimerTriggerCause.class)) {
      return Messages.SonarPublisher_TimerBuild();
    } else if (!isUserBuilds() && isTrigger(build, Cause.UserCause.class)) {
      return Messages.SonarPublisher_UserBuild();
    } else if (!isSnapshotDependencyBuilds() && isTrigger(build, Cause.UpstreamCause.class)) {
      return Messages.SonarPublisher_SnapshotDepBuild();
    }
    return null;
  }

  /**
   * Returns true, if specified build triggered by specified trigger.
   *
   * @param build   build
   * @param trigger trigger
   * @return true, if specified build triggered by specified trigger
   */
  private static boolean isTrigger(AbstractBuild<?, ?> build, Class<? extends hudson.model.Cause> trigger) {
    CauseAction buildCause = build.getAction(CauseAction.class);
    List<Cause> buildCauses = buildCause.getCauses();
    for (Cause cause : buildCauses) {
      if (trigger.isInstance(cause)) {
        return true;
      }
    }
    return false;
  }
}
