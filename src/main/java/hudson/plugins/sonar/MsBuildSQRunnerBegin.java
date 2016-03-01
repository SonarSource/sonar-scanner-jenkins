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

import hudson.Util;
import hudson.util.FormValidation;
import org.kohsuke.stapler.QueryParameter;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.plugins.sonar.utils.BuilderUtils;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import jenkins.model.Jenkins;
import org.codehaus.plexus.util.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nullable;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class MsBuildSQRunnerBegin extends AbstractMsBuildSQRunner {
  private final String msBuildRunnerInstallationName;
  private final String sonarInstallationName;
  private final String projectKey;
  private final String projectName;
  private final String projectVersion;
  private final String additionalArguments;

  @DataBoundConstructor
  public MsBuildSQRunnerBegin(String msBuildRunnerInstallationName, String sonarInstallationName, String projectKey, String projectName, String projectVersion,
    String additionalArguments) {
    this.msBuildRunnerInstallationName = msBuildRunnerInstallationName;
    this.sonarInstallationName = sonarInstallationName;
    this.projectKey = projectKey;
    this.projectName = projectName;
    this.projectVersion = projectVersion;
    this.additionalArguments = additionalArguments;
  }

  @Override
  public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
    ArgumentListBuilder args = new ArgumentListBuilder();

    EnvVars env = BuilderUtils.getEnvAndBuildVars(run, listener);
    SonarInstallation sonarInstallation = getSonarInstallation(getSonarInstallationName(), listener);
    saveScannerName(run, msBuildRunnerInstallationName);
    saveSonarInstanceName(run, getSonarInstallationName());

    MsBuildSQRunnerInstallation msBuildScanner = getDescriptor().getMsBuildScannerInstallation(msBuildRunnerInstallationName);
    args.add(getExeName(msBuildScanner, env, launcher, listener));
    Map<String, String> props = getSonarProps(sonarInstallation);
    addArgsTo(args, sonarInstallation, env, props);

    int result = launcher.launch().cmds(args).envs(env).stdout(listener).pwd(BuilderUtils.getModuleRoot(run, workspace)).join();

    if (result != 0) {
      throw new AbortException(Messages.MSBuildScanner_ExecFailed(result));
    }
  }

  private static Map<String, String> getSonarProps(SonarInstallation inst) {
    Map<String, String> map = new LinkedHashMap<String, String>();

    map.put("sonar.host.url", inst.getServerUrl());

    if (!StringUtils.isBlank(inst.getServerAuthenticationToken())) {
      map.put("sonar.login", inst.getServerAuthenticationToken());
    } else {
      map.put("sonar.login", inst.getSonarLogin());
      map.put("sonar.password", inst.getSonarPassword());
    }

    map.put("sonar.jdbc.url", inst.getDatabaseUrl());
    map.put("sonar.jdbc.username", inst.getDatabaseLogin());
    map.put("sonar.jdbc.password", inst.getDatabasePassword());

    return map;
  }

  private void addArgsTo(ArgumentListBuilder args, SonarInstallation sonarInst, EnvVars env, Map<String, String> props) {
    args.add("begin");

    args.add("/k:" + projectKey + "");
    args.add("/n:" + projectName + "");
    args.add("/v:" + projectVersion + "");

    // expand macros using itself
    EnvVars.resolve(props);

    for (Map.Entry<String, String> e : props.entrySet()) {
      if (!StringUtils.isEmpty(e.getValue())) {
        // expand macros using environment variables and hide passwords
        args.addKeyValuePair("/d:", e.getKey(), env.expand(e.getValue()), e.getKey().contains("password"));
      }
    }

    args.add(sonarInst.getAdditionalAnalysisPropertiesWindows());
    args.addTokenized(sonarInst.getAdditionalProperties());
    args.addTokenized(additionalArguments);
  }

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  /*
   * FOR UI
   */
  public String getProjectKey() {
    return Util.fixEmptyAndTrim(projectKey);
  }

  public String getProjectVersion() {
    return Util.fixEmptyAndTrim(projectVersion);
  }

  public String getProjectName() {
    return Util.fixEmptyAndTrim(projectName);
  }

  public String getSonarInstallationName() {
    return Util.fixEmptyAndTrim(sonarInstallationName);
  }

  public String getMsBuildScannerInstallationName() {
    return Util.fixEmptyAndTrim(msBuildRunnerInstallationName);
  }

  public String getAdditionalArguments() {
    return Util.fixEmptyAndTrim(additionalArguments);
  }

  /*
   * 
   */
  @Extension
  public static class DescriptorImpl extends BuildStepDescriptor<Builder> {
    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
      return true;
    }

    @Override
    public String getHelpFile() {
      return "/plugin/sonar/help-ms-build-sq-scanner-begin.html";
    }

    @Override
    public String getDisplayName() {
      return Messages.MsBuildScannerBegin_DisplayName();
    }

    public FormValidation doCheckProjectKey(@QueryParameter String value) {
      return checkNotEmpty(value);
    }

    public FormValidation doCheckProjectName(@QueryParameter String value) {
      return checkNotEmpty(value);
    }

    public FormValidation doCheckProjectVersion(@QueryParameter String value) {
      return checkNotEmpty(value);
    }

    private static FormValidation checkNotEmpty(String value) {
      if (!StringUtils.isEmpty(value)) {
        return FormValidation.ok();
      }
      return FormValidation.error(Messages.SonarPublisher_MandatoryProperty());
    }

    @Nullable
    public MsBuildSQRunnerInstallation getMsBuildScannerInstallation(String name) {
      MsBuildSQRunnerInstallation[] msInst = getMsBuildScannerInstallations();

      if (name == null && msInst.length > 0) {
        return msInst[0];
      }

      for (MsBuildSQRunnerInstallation inst : msInst) {
        if (StringUtils.equals(name, inst.getName())) {
          return inst;
        }
      }

      return null;
    }

    public MsBuildSQRunnerInstallation[] getMsBuildScannerInstallations() {
      return Jenkins.getInstance().getDescriptorByType(MsBuildSQRunnerInstallation.DescriptorImpl.class).getInstallations();
    }

    public SonarInstallation[] getSonarInstallations() {
      return SonarInstallation.all();
    }
  }

}
