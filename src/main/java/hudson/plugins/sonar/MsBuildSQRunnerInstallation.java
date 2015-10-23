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

import com.google.common.annotations.VisibleForTesting;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import hudson.model.EnvironmentSpecific;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolInstallation;
import jenkins.security.MasterToSlaveCallable;
import hudson.Launcher;
import hudson.Extension;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstaller;
import hudson.Util;
import hudson.tools.ToolProperty;
import org.kohsuke.stapler.DataBoundConstructor;
import hudson.EnvVars;
import hudson.model.TaskListener;
import hudson.model.Node;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class MsBuildSQRunnerInstallation extends ToolInstallation implements EnvironmentSpecific<MsBuildSQRunnerInstallation>, NodeSpecific<MsBuildSQRunnerInstallation> {
  private static final String EXE_NAME = "MSBuild.SonarQube.Runner.exe";
  private static final long serialVersionUID = 1L;
  private final String exeName;

  @DataBoundConstructor
  public MsBuildSQRunnerInstallation(String name, String home, List<? extends ToolProperty<?>> properties) {
    this(Util.fixEmptyAndTrim(name), Util.fixEmptyAndTrim(home), properties, EXE_NAME);
  }

  @VisibleForTesting
  public MsBuildSQRunnerInstallation(String name, String home, List<? extends ToolProperty<?>> properties, String exeName) {
    super(Util.fixEmptyAndTrim(name), Util.fixEmptyAndTrim(home), properties);
    this.exeName = exeName;
  }

  @Override
  public MsBuildSQRunnerInstallation forEnvironment(EnvVars environment) {
    return new MsBuildSQRunnerInstallation(getName(), environment.expand(getHome()), getProperties().toList());
  }

  @Override
  public MsBuildSQRunnerInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
    return new MsBuildSQRunnerInstallation(getName(), translateFor(node, log), getProperties().toList());
  }

  @Extension
  public static class DescriptorImpl extends ToolDescriptor<MsBuildSQRunnerInstallation> {
    // super already manages: persistence of installations and form json deserialization
    public DescriptorImpl() {
      super();
      load();
    }

    @Override
    public void setInstallations(MsBuildSQRunnerInstallation... installations) {
      super.setInstallations(installations);
      save();
    }

    @Override
    public MsBuildSQRunnerInstallation newInstance(StaplerRequest req, JSONObject formData) {
      return (MsBuildSQRunnerInstallation) req.bindJSON(clazz, formData);
    }

    @Override
    public String getDisplayName() {
      return "MSBuild SonarQube Runner";
    }

    @Override
    public List<? extends ToolInstaller> getDefaultInstallers() {
      return Collections.singletonList(new MsBuildSonarQubeRunnerInstaller(null));
    }
  }

  public String getExecutable(Launcher launcher) throws IOException, InterruptedException {
    return launcher.getChannel().call(new MasterToSlaveCallable<String, IOException>() {
      private static final long serialVersionUID = 1L;

      @Override
      public String call() throws IOException {
        String home = Util.replaceMacro(getHome(), EnvVars.masterEnvVars);
        File exe = new File(home, exeName);
        if (exe.exists()) {
          return exe.getPath();
        }
        return null;
      }
    });
  }
}
