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
package hudson.plugins.sonar.utils;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Computer;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.SCM;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolInstallation;
import java.io.IOException;
import java.util.Collection;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import jenkins.triggers.SCMTriggerItem;

public class BuilderUtils {
  private BuilderUtils() {
    // only static
  }

  @CheckForNull
  public static <T extends ToolInstallation & EnvironmentSpecific<T> & NodeSpecific<T>> T getBuildTool(@Nullable T tool, EnvVars env, TaskListener listener, FilePath workspace)
    throws IOException,
    InterruptedException {
    Computer computer = workspace.toComputer();
    if (computer == null) {
      return null;
    }
    Node node = computer.getNode();
    if (tool == null || node == null) {
      return null;
    }
    T t = tool.forNode(node, listener);
    t = t.forEnvironment(env);

    return t;
  }

  /**
   * Get environment vars of the run, with all values overridden by build vars
   */
  public static EnvVars getEnvAndBuildVars(Run<?, ?> run, TaskListener listener) throws IOException, InterruptedException {
    EnvVars env = run.getEnvironment(listener);
    if (run instanceof AbstractBuild) {
      env.overrideAll(((AbstractBuild<?, ?>) run).getBuildVariables());
    }
    return env;
  }

  public static FilePath getModuleRoot(Run<?, ?> run, FilePath workspace) {
    FilePath moduleRoot = null;

    if (run instanceof AbstractBuild) {
      AbstractBuild<?, ?> build = (AbstractBuild<?, ?>) run;
      moduleRoot = build.getModuleRoot();
    } else {
      // otherwise get the first module of the first SCM
      Object parent = run.getParent();
      if (parent instanceof SCMTriggerItem) {
        SCMTriggerItem scmTrigger = (SCMTriggerItem) parent;
        Collection<? extends SCM> scms = scmTrigger.getSCMs();
        if (!scms.isEmpty()) {
          SCM scm = scms.iterator().next();
          FilePath[] moduleRoots = scm.getModuleRoots(workspace, null);
          moduleRoot = moduleRoots != null && moduleRoots.length > 0 ? moduleRoots[0] : null;
        }
      }
      if (moduleRoot == null) {
        moduleRoot = workspace;
      }
    }
    return moduleRoot;
  }
}
