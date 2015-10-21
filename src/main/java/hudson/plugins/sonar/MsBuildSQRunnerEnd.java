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

import hudson.plugins.sonar.action.UrlSonarAction;
import hudson.plugins.sonar.utils.SonarUtils;

import hudson.plugins.sonar.action.BuildSonarAction;
import hudson.model.Action;
import hudson.plugins.sonar.action.ProjectSonarAction;
import org.kohsuke.stapler.DataBoundConstructor;
import jenkins.model.Jenkins;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.plugins.sonar.utils.BuilderUtils;
import hudson.util.ArgumentListBuilder;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.model.Run;

import java.io.IOException;

import hudson.tasks.Builder;

public class MsBuildSQRunnerEnd extends AbstractMsBuildSQRunner {
  @DataBoundConstructor
  public MsBuildSQRunnerEnd() {
    // will use MSBuild SQ Runner installation defined in Begin
  }

  @Override
  public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
    ArgumentListBuilder args = new ArgumentListBuilder();

    EnvVars env = BuilderUtils.getEnvAndBuildVars(run, listener);
    String runnerName = loadRunnerName(env);
    MsBuildSQRunnerInstallation msBuildRunner = Jenkins.getInstance().getDescriptorByType(MsBuildSQRunnerBegin.DescriptorImpl.class)
      .getMsBuildRunnerInstallation(runnerName);
    msBuildRunner = BuilderUtils.getBuildTool(msBuildRunner, env, listener);

    String exe;
    if (msBuildRunner != null) {
      exe = msBuildRunner.getExecutable(launcher);
      if (exe == null) {
        throw new AbortException(Messages.MsBuildRunner_ExecutableNotFound(msBuildRunner.getName()));
      }
    } else {
      listener.getLogger().println(Messages.MsBuildRunner_NoInstallation());
      exe = EXE;
    }

    args.add(exe);
    args.add("end");

    int result = launcher.launch().cmds(args).envs(env).stdout(listener).pwd(BuilderUtils.getModuleRoot(run, workspace)).join();

    if (result != 0) {
      throw new AbortException(Messages.MSBuildRunner_ExecFailed(result));
    }

    addBadge(run);
  }

  private static void addBadge(Run<?, ?> run) throws IOException {
    UrlSonarAction urlAction = SonarUtils.addUrlActionTo(run);
    String url = urlAction != null ? urlAction.getSonarUrl() : null;

    if (run.getAction(BuildSonarAction.class) == null) {
      run.addAction(new BuildSonarAction(url));
    }
  }

  @Override
  public Action getProjectAction(AbstractProject<?, ?> project) {
    return new ProjectSonarAction(project);
  }

  @Extension
  public static class DescriptorImpl extends BuildStepDescriptor<Builder> {
    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
      return true;
    }

    @Override
    public String getHelpFile() {
      return "/plugin/sonar/help-ms-build-sq-runner-end.html";
    }

    @Override
    public String getDisplayName() {
      return Messages.MsBuildRunnerEnd_DisplayName();
    }
  }
}
