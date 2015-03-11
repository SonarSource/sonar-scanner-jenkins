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

import hudson.*;
import hudson.model.EnvironmentSpecific;
import hudson.model.TaskListener;
import hudson.model.Node;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import jenkins.security.MasterToSlaveCallable;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
* Represents a Sonar runner installation in a system.
*/
public class SonarRunnerInstallation extends ToolInstallation implements EnvironmentSpecific<SonarRunnerInstallation>, NodeSpecific<SonarRunnerInstallation> {

  @DataBoundConstructor
  public SonarRunnerInstallation(String name, String home, List<? extends ToolProperty<?>> properties) {
    super(Util.fixEmptyAndTrim(name), Util.fixEmptyAndTrim(home), properties);
  }

  /**
  * Gets the executable path of this Sonar runner on the given target system.
  */
  public String getExecutable(Launcher launcher) throws IOException, InterruptedException {
    return launcher.getChannel().call(new MasterToSlaveCallable<String, IOException>() {
      @Override
      public String call() throws IOException {
        File exe = getExeFile();
        if (exe.exists()) {
          return exe.getPath();
        }
        return null;
      }
    });
  }

  private File getExeFile() {
    String execName = Functions.isWindows() ? "sonar-runner.bat" : "sonar-runner";
    String home = Util.replaceMacro(getHome(), EnvVars.masterEnvVars);

    return new File(home, "bin/" + execName);
  }

  private static final long serialVersionUID = 1L;

  @Override
  public SonarRunnerInstallation forEnvironment(EnvVars environment) {
    return new SonarRunnerInstallation(getName(), environment.expand(getHome()), getProperties().toList());
  }

  @Override
  public SonarRunnerInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
    return new SonarRunnerInstallation(getName(), translateFor(node, log), getProperties().toList());
  }

  @Extension
  public static class DescriptorImpl extends ToolDescriptor<SonarRunnerInstallation> {
    @CopyOnWrite
    private volatile SonarRunnerInstallation[] installations = new SonarRunnerInstallation[0];

    public DescriptorImpl() {
      load();
    }

    @Override
    public String getDisplayName() {
      return "SonarQube Runner";
    }

    @Override
    public List<? extends ToolInstaller> getDefaultInstallers() {
      return Collections.singletonList(new SonarRunnerInstaller(null));
    }

    @Override
    public SonarRunnerInstallation[] getInstallations() {
      return installations;
    }

    @Override
    public SonarRunnerInstallation newInstance(StaplerRequest req, JSONObject formData) {
      return (SonarRunnerInstallation) req.bindJSON(clazz, formData);
    }

    public void setInstallations(SonarRunnerInstallation... antInstallations) {
      this.installations = antInstallations;
      save();
    }

  }

}
