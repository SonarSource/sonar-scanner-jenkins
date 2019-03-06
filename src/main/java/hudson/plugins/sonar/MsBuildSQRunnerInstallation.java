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

import com.google.common.annotations.VisibleForTesting;
import hudson.EnvVars;
import hudson.Extension;
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
import org.kohsuke.stapler.StaplerRequest;

public class MsBuildSQRunnerInstallation extends ToolInstallation implements EnvironmentSpecific<MsBuildSQRunnerInstallation>, NodeSpecific<MsBuildSQRunnerInstallation> {
  private static final long serialVersionUID = 1L;
  static final String SCANNER_EXE_NAME = "SonarScanner.MSBuild.exe";
  static final String SCANNER_DLL_NAME = "SonarScanner.MSBuild.dll";
  static final String OLD_SCANNER_EXE_NAME = "MSBuild.SonarQube.Runner.exe";
  private static String testExeName = null;

  @DataBoundConstructor
  public MsBuildSQRunnerInstallation(String name, String home, List<? extends ToolProperty<?>> properties) {
    super(Util.fixEmptyAndTrim(name), Util.fixEmptyAndTrim(home), properties);
  }

  @VisibleForTesting
  static void setTestExeName(String exe) {
    testExeName = exe;
  }

  @VisibleForTesting
  static String getScannerToolPath(String home) {
    if (testExeName != null) {
      File testScanner = new File(home, testExeName);
      return testScanner.getPath();
    }

    File scannerNet46 = new File(home, SCANNER_EXE_NAME);
    if (scannerNet46.exists()) {
      return scannerNet46.getPath();
    }

    File scannerNetCore = new File(home, SCANNER_DLL_NAME);
    if (scannerNetCore.exists()) {
      return scannerNetCore.getPath();
    }

    File oldScanner = new File(home, OLD_SCANNER_EXE_NAME);
    if (oldScanner.exists()) {
      return oldScanner.getPath();
    }

    return null;
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
      return "SonarScanner for MSBuild";
    }

    @Override
    public List<? extends ToolInstaller> getDefaultInstallers() {
      return Collections.singletonList(new MsBuildSonarQubeRunnerInstaller(null));
    }
  }

  public static String getScannerName() {
    if (testExeName != null) {
      return testExeName;
    } else {
      return SCANNER_EXE_NAME;
    }
  }

  public String getToolPath(Launcher launcher) throws IOException, InterruptedException {
    return launcher.getChannel().call(new MasterToSlaveCallable<String, IOException>() {
      private static final long serialVersionUID = 1L;

      @Override
      public String call() {
        String home = Util.replaceMacro(getHome(), EnvVars.masterEnvVars);
        return getScannerToolPath(home);
      }
    });
  }
}
