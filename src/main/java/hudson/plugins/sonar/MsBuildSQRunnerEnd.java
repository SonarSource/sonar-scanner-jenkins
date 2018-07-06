/*
 * SonarQube Scanner for Jenkins
 * Copyright (C) 2007-2019 SonarSource SA
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
package hudson.plugins.sonar;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.sonar.action.SonarMarkerAction;
import hudson.plugins.sonar.utils.BuilderUtils;
import hudson.plugins.sonar.utils.SonarUtils;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

public class MsBuildSQRunnerEnd extends AbstractMsBuildSQRunner {
  @DataBoundConstructor
  public MsBuildSQRunnerEnd() {
    // will use MSBuild SQ Scanner installation defined in Begin
  }

  @Override
  public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
    ArgumentListBuilder args = new ArgumentListBuilder();

    EnvVars env = BuilderUtils.getEnvAndBuildVars(run, listener);
    SonarQubeScannerMsBuildParams beginParams = run.getAction(SonarQubeScannerMsBuildParams.class);
    if (beginParams == null) {
      throw new AbortException(Messages.MsBuildScannerEnd_NoBeginStep());
    }

    String scannerName = beginParams.getScannerName();
    String sonarInstName = beginParams.getSqServerName();
    SonarInstallation sonarInstallation = getSonarInstallation(sonarInstName, listener);

    MsBuildSQRunnerInstallation msBuildScanner = Jenkins.getInstance().getDescriptorByType(MsBuildSQRunnerBegin.DescriptorImpl.class)
      .getMsBuildScannerInstallation(scannerName);
    String scannerPath = getScannerPath(msBuildScanner, env, launcher, listener, workspace);
    if (isDotNetCoreTool(scannerPath)) {
      addDotNetCommand(args);
    }
    args.add(scannerPath);
    addArgs(args, env, sonarInstallation, run);

    int result = launcher.launch().cmds(args).envs(env).stdout(listener).pwd(BuilderUtils.getModuleRoot(run, workspace)).join();

    if (result != 0) {
      addBadge(run, listener, workspace, sonarInstallation);
      throw new AbortException(Messages.MSBuildScanner_ExecFailed(result));
    }

    addBadge(run, listener, workspace, sonarInstallation);
  }

  private static void addArgs(ArgumentListBuilder args, EnvVars env, SonarInstallation sonarInstallation, Run<?, ?> run) {
    Map<String, String> props = getSonarProps(sonarInstallation, run);

    args.add("end");

    // expand macros using itself
    EnvVars.resolve(props);

    for (Map.Entry<String, String> e : props.entrySet()) {
      if (!StringUtils.isEmpty(e.getValue())) {
        // expand macros using environment variables and hide token
        boolean hide = e.getKey().contains("sonar.login");
        args.addKeyValuePair("/d:", e.getKey(), env.expand(e.getValue()), hide);
      }
    }

    args.addTokenized(sonarInstallation.getAdditionalProperties());
  }

  private static Map<String, String> getSonarProps(SonarInstallation inst, Run<?, ?> run) {
    Map<String, String> map = new LinkedHashMap<>();

    String token = inst.getServerAuthenticationToken(run);
    if (!StringUtils.isBlank(token)) {
      map.put("sonar.login", token);
    }

    return map;
  }

  private static void addBadge(Run<?, ?> run, TaskListener listener, FilePath workspace, SonarInstallation sonarInstallation) throws IOException, InterruptedException {
    SonarUtils.addBuildInfoTo(run, listener, workspace, sonarInstallation.getName());
  }

  @Override
  public Action getProjectAction(AbstractProject<?, ?> project) {
    return new SonarMarkerAction();
  }

  @Extension
  public static class DescriptorImpl extends BuildStepDescriptor<Builder> {
    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
      return true;
    }

    @Override
    public String getHelpFile() {
      return "/plugin/sonar/help-ms-build-sq-scanner-end.html";
    }

    @Override
    public String getDisplayName() {
      return Messages.MsBuildScannerEnd_DisplayName();
    }
  }
}
