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
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.Result;
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

  private boolean descendantOfUserBuilds;

  private boolean snapshotDependencyBuilds;

  private boolean skipIfBuildFails;

  public TriggersConfig() {
  }

  @DataBoundConstructor
  public TriggersConfig(boolean scmBuilds, boolean timerBuilds, boolean userBuilds, boolean descendantOfUserBuilds, boolean snapshotDependencyBuilds, boolean skipIfBuildFails) {
    this.scmBuilds = scmBuilds;
    this.timerBuilds = timerBuilds;
    this.userBuilds = userBuilds;
    this.descendantOfUserBuilds = descendantOfUserBuilds;
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

  public boolean isDescendantOfUserBuilds() {
    return descendantOfUserBuilds;
  }

  public void setDescendantOfUserBuilds(boolean descendantOfUserBuilds) {
    this.descendantOfUserBuilds = descendantOfUserBuilds;
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

  /**
   * Returns {@code true} if the Sonar analysis has to be performed based on the
   * triggers configuration, {@code false} otherwise.
   */
  public boolean doPerformSonar(AbstractBuild<?, ?> build) {
    if (build.getResult().isWorseThan(Result.FAILURE) ||
            isSkipIfBuildFails() && build.getResult().isWorseThan(Result.UNSTABLE)) { // Skip analysis on build failure
      return false;
    }
    else if (isScmBuilds() && isTrigger(build, SCMTrigger.SCMTriggerCause.class) || // Poll SCM
            isTimerBuilds() && isTrigger(build, TimerTrigger.TimerTriggerCause.class) || // Build periodically
            isUserBuilds() && isTrigger(build, Cause.UserCause.class) || // Manually started by user
            isDescendantOfUserBuilds() && isAscendantTrigger(build, Cause.UserCause.class) || // Descendant of a build manually started by user
            isSnapshotDependencyBuilds() && isTrigger(build, Cause.UpstreamCause.class)) { // Build whenever a SNAPSHOT dependency is built
      return true;
    }

    return false;
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

  /**
   * Returns true if the specified build has been triggered, or one of its ascendant,
   * by the specified {@code trigger}.
   *
   * @param build Build to check
   * @param trigger Trigger for the specified build
   */
  private static boolean isAscendantTrigger(AbstractBuild<?, ?> build, Class<? extends hudson.model.Cause> trigger) {
    try {
      AbstractBuild currentBuild = build;
      List<Cause> currentCauses = currentBuild.getAction(CauseAction.class).getCauses();

      while(currentCauses.size() == 1) {
        Cause cause = currentCauses.get(0);
        if(cause instanceof Cause.UpstreamCause) {
          // let's move to the upstream build
          String currentProject = ((Cause.UpstreamCause) cause).getUpstreamProject();
          currentBuild = (AbstractBuild) ((Job) Hudson.getInstance().getItem(currentProject)).getBuildByNumber(((Cause.UpstreamCause) cause).getUpstreamBuild());
          currentCauses = currentBuild.getAction(CauseAction.class).getCauses();
        }
        else if(trigger.isInstance(cause)) {
          // the trigger is the expected one
          return true;
        }
        else {
          // another cause, let's make all this fail then
          break;
        }
      }
    }
    catch(Exception e) {
      // it would be far too complicated to handle all the potential cases that
      // may occur about upstream builds, so let's handle that through a basic
      // exception
    }

    return false;
  }

}
