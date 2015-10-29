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

import hudson.plugins.sonar.utils.BuilderUtils;

import hudson.Launcher;
import hudson.model.TaskListener;
import org.codehaus.plexus.util.StringUtils;
import hudson.Util;
import hudson.model.Run;
import hudson.model.AbstractBuild;
import hudson.model.EnvironmentContributingAction;
import hudson.model.InvisibleAction;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import hudson.AbortException;
import hudson.EnvVars;

import javax.annotation.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public abstract class AbstractMsBuildSQRunner extends Builder implements SimpleBuildStep {
  protected static final String EXE = "MSBuild.SonarQube.Runner.exe";
  static final String INST_NAME_KEY = "msBuildRunnerInstallationName";
  static final String SONAR_INST_NAME_KEY = "sonarInstanceName";

  @Nullable
  protected static String loadRunnerName(EnvVars env) throws IOException, InterruptedException {
    if (!env.containsKey(INST_NAME_KEY)) {
      throw new AbortException(Messages.MsBuildRunnerEnd_NoInstallationName());
    }

    String name = env.get(INST_NAME_KEY);
    if (StringUtils.isEmpty(name)) {
      return null;
    }
    return name;
  }

  protected static SonarInstallation getSonarInstallation(String instName, TaskListener listener) throws AbortException {
    if (!SonarInstallation.isValid(instName, listener)) {
      throw new AbortException();
    }
    return SonarInstallation.get(instName);
  }

  @Nullable
  protected static String loadSonarInstanceName(EnvVars env) throws IOException, InterruptedException {
    if (!env.containsKey(SONAR_INST_NAME_KEY)) {
      throw new AbortException(Messages.MsBuildRunnerEnd_NoSonarInstanceName());
    }

    String name = env.get(SONAR_INST_NAME_KEY);
    if (StringUtils.isEmpty(name)) {
      return null;
    }
    return name;
  }

  protected String getExeName(MsBuildSQRunnerInstallation msBuildRunner, EnvVars env, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
    MsBuildSQRunnerInstallation inst = BuilderUtils.getBuildTool(msBuildRunner, env, listener);

    String exe;
    if (inst != null) {
      exe = inst.getExecutable(launcher);
      if (exe == null) {
        throw new AbortException(Messages.MsBuildRunner_ExecutableNotFound(inst.getName()));
      }
    } else {
      listener.getLogger().println(Messages.MsBuildRunner_NoInstallation());
      exe = EXE;
    }

    return exe;
  }

  protected static void saveRunnerName(Run<?, ?> r, @Nullable String name) throws IOException, InterruptedException {
    r.addAction(new InjectEnvVarAction(Collections.singletonMap(INST_NAME_KEY, Util.fixNull(name))));
  }

  protected static void saveSonarInstanceName(Run<?, ?> r, @Nullable String name) throws IOException, InterruptedException {
    r.addAction(new InjectEnvVarAction(Collections.singletonMap(SONAR_INST_NAME_KEY, Util.fixNull(name))));
  }

  protected static class InjectEnvVarAction extends InvisibleAction implements EnvironmentContributingAction {
    private final Map<String, String> vars;

    InjectEnvVarAction(Map<String, String> vars) {
      this.vars = vars;
    }

    @Override
    public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {
      env.putAll(vars);
    }
  }
}
