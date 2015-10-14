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

import org.apache.commons.lang.StringUtils;
import hudson.plugins.sonar.utils.MaskPasswordsOutputStream;
import hudson.model.Run.RunnerAbortedException;
import hudson.EnvVars;
import hudson.plugins.sonar.utils.Logger;

import javax.annotation.Nullable;

import org.kohsuke.stapler.DataBoundConstructor;
import hudson.CopyOnWrite;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SonarBuildWrapper extends BuildWrapper {
  private String installationName = null;

  @DataBoundConstructor
  public SonarBuildWrapper(@Nullable String installationName) {
    this.installationName = installationName;
  }

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  @Override
  public OutputStream decorateLogger(AbstractBuild build, OutputStream outputStream) throws IOException, InterruptedException, RunnerAbortedException {
    SonarInstallation inst = getInstallation();
    if (inst == null || (inst.getDatabasePassword() == null && inst.getSonarPassword() == null)) {
      return outputStream;
    }

    Logger.LOG.info("Masking sonar passwords");

    List<String> passwords = new ArrayList<String>();

    if (inst.getDatabasePassword() != null) {
      passwords.add(inst.getDatabasePassword());
    }
    if (inst.getSonarPassword() != null) {
      passwords.add(inst.getSonarPassword());
    }

    return new MaskPasswordsOutputStream(outputStream, passwords);
  }

  /**
   * @return name of {@link hudson.plugins.sonar.SonarInstallation}
   */
  public @Nullable String getInstallationName() {
    return installationName;
  }

  public void setInstallationName(@Nullable String installationName) {
    this.installationName = installationName;
  }

  public @Nullable SonarInstallation getInstallation() {
    return SonarInstallation.get(getInstallationName());
  }

  @Override
  public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
    if (!SonarInstallation.isValid(getInstallationName(), listener)) {
      return new Environment() {
      };
    }
    return new SonarEnvironment(getInstallation(), listener.getLogger());
  }

  @Extension
  public static final class DescriptorImpl extends BuildWrapperDescriptor
  {
    @CopyOnWrite
    private volatile SonarInstallation[] installations = new SonarInstallation[0];

    @Override
    public String getDisplayName() {
      return Messages.SonarBuildWrapper_DisplayName();
    }

    @Override
    public boolean isApplicable(AbstractProject<?, ?> item) {
      return true;
    }

    /**
     * @return all configured {@link hudson.plugins.sonar.SonarInstallation}
     */
    public SonarInstallation[] getSonarInstallations() {
      return SonarInstallation.enabled();
    }

    @Override
    public String getHelpFile() {
      return "/plugin/sonar/help-buildWrapper.html";
    }
  }

  class SonarEnvironment extends Environment {
    private final SonarInstallation installation;
    private final PrintStream buildLogger;

    SonarEnvironment(SonarInstallation installation, PrintStream buildLogger) {
      this.installation = installation;
      this.buildLogger = buildLogger;
    }

    @Override
    public void buildEnvVars(Map<String, String> env) {
      String l = "Injecting SonarQube environment variables using the configuration: " + installation.getName();
      Logger.LOG.info(l);
      buildLogger.println("[SONAR]" + l);

      Map<String, String> sonarEnv = createVars(installation);

      // resolve variables against each other
      Map<String, String> sonarEnvResolved = new HashMap<String, String>(sonarEnv);
      EnvVars.resolve(sonarEnvResolved);

      for (String k : sonarEnv.keySet()) {
        String v = sonarEnvResolved.get(k);
        Logger.LOG.info("  Setting '" + k + "'");
        env.put(k, v);
      }
    }

    private Map<String, String> createVars(SonarInstallation inst) {
      Map<String, String> map = new HashMap<String, String>();

      map.put("SONAR_CONFIG_NAME", inst.getName());
      map.put("SONAR_HOST_URL", getOrDefault(inst.getServerUrl(), "http://localhost:9000"));

      map.put("SONAR_LOGIN", getOrDefault(inst.getSonarLogin(), "sonar"));
      map.put("SONAR_PASSWORD", getOrDefault(inst.getSonarPassword(), "sonar"));

      map.put("SONAR_JDBC_URL", getOrDefault(inst.getDatabaseUrl(), ""));
      map.put("SONAR_JDBC_USERNAME", getOrDefault(inst.getDatabaseLogin(), ""));
      map.put("SONAR_JDBC_PASSWORD", getOrDefault(inst.getDatabasePassword(), ""));

      if (StringUtils.isEmpty(inst.getMojoVersion())) {
        map.put("SONAR_MAVEN_GOAL", "sonar:sonar");
      } else {
        map.put("SONAR_MAVEN_GOAL", "org.codehaus.mojo:sonar-maven-plugin:" + installation.getMojoVersion() + ":sonar");
      }

      map.put("SONAR_ADDITIONAL", getOrDefault(inst.getAdditionalProperties(), ""));

      return map;
    }

    private String getOrDefault(String value, String defaultValue) {
      return !StringUtils.isEmpty(value) ? value : defaultValue;
    }
  }
}
