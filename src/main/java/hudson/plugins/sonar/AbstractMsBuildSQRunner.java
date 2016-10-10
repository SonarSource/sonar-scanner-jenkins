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

import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.InvisibleAction;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.sonar.utils.BuilderUtils;
import hudson.tasks.Builder;
import java.io.IOException;
import javax.annotation.Nullable;

public abstract class AbstractMsBuildSQRunner extends Builder {
  protected static final String EXE = "MSBuild.SonarQube.Runner.exe";
  static final String INST_NAME_KEY = "msBuildScannerInstallationName";
  static final String SONAR_INST_NAME_KEY = "sonarInstanceName";

  protected static SonarInstallation getSonarInstallation(String instName, TaskListener listener) throws AbortException {
    if (!SonarInstallation.isValid(instName, listener)) {
      throw new AbortException();
    }
    return SonarInstallation.get(instName);
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
    FilePath workspace = build.getWorkspace();
    if (workspace == null) {
      throw new AbortException("no workspace for " + build);
    }
    perform(build, workspace, launcher, listener);
    return true;
  }

  protected abstract void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException;

  protected String getExeName(MsBuildSQRunnerInstallation msBuildScanner, EnvVars env, Launcher launcher, TaskListener listener, FilePath workspace)
    throws IOException, InterruptedException {
    MsBuildSQRunnerInstallation inst = BuilderUtils.getBuildTool(msBuildScanner, env, listener, workspace);

    String exe;
    if (inst != null) {
      exe = inst.getExecutable(launcher);
      if (exe == null) {
        throw new AbortException(Messages.MsBuildScanner_ExecutableNotFound(inst.getName()));
      }
    } else {
      listener.getLogger().println(Messages.MsBuildScanner_NoInstallation());
      exe = EXE;
    }

    return exe;
  }

  static class SonarQubeScannerMsBuildParams extends InvisibleAction {

    private final String scannerName;
    private final String sqServerName;

    SonarQubeScannerMsBuildParams(@Nullable String scannerName, @Nullable String sqServerName) {
      this.scannerName = Util.fixNull(scannerName);
      this.sqServerName = Util.fixNull(sqServerName);
    }

    public String getScannerName() {
      return scannerName;
    }

    public String getSqServerName() {
      return sqServerName;
    }

  }
}
