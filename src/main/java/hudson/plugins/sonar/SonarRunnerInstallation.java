/*
 * SonarQube Scanner for Jenkins
 * Copyright (C) 2007-2025 SonarSource SA
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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.CopyOnWrite;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Functions;
import hudson.Launcher;
import hudson.Util;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import jenkins.security.MasterToSlaveCallable;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest2;

/**
 * Represents a SonarQube Scanner installation in a system.
 */
public class SonarRunnerInstallation extends ToolInstallation implements EnvironmentSpecific<SonarRunnerInstallation>, NodeSpecific<SonarRunnerInstallation> {
  private static final long serialVersionUID = 1L;

  @DataBoundConstructor
  public SonarRunnerInstallation(String name, @CheckForNull String home, List<? extends ToolProperty<?>> properties) {
    super(Util.fixEmptyAndTrim(name), Util.fixEmptyAndTrim(home), properties);
  }

  /**
   * Gets the executable path of this SonarQube Scanner on the given target system.
   */
  public String getExecutable(Launcher launcher) throws IOException, InterruptedException {
    return launcher.getChannel().call(new GetExecutable(getHome()));
  }

  private static class GetExecutable extends MasterToSlaveCallable<String, IOException> {
    private final String rawHome;

    GetExecutable(String rawHome) {
      this.rawHome = rawHome;
    }

    @Override
    public String call() throws IOException {
      File exe = getExeFile("sonar-scanner", rawHome);
      if (exe.exists()) {
        return exe.getPath();
      }
      File oldExe = getExeFile("sonar-runner", rawHome);
      if (oldExe.exists()) {
        return oldExe.getPath();
      }
      return null;
    }

    private static File getExeFile(String name, String rawHome) {
      String execName = Functions.isWindows() ? (name + ".bat") : name;
      String home = Util.replaceMacro(rawHome, EnvVars.masterEnvVars);

      return new File(home, "bin/" + execName);
    }
  }

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

    @NonNull
    @Override
    public String getDisplayName() {
      return "SonarQube Scanner";
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
    public SonarRunnerInstallation newInstance(StaplerRequest2 req, JSONObject formData) {
      return (SonarRunnerInstallation) req.bindJSON(clazz, formData);
    }

    public void setInstallations(SonarRunnerInstallation... antInstallations) {
      this.installations = antInstallations;
      save();
    }

  }

}
