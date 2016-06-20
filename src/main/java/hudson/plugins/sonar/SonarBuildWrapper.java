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

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.plugins.sonar.action.SonarMarkerAction;
import hudson.plugins.sonar.utils.Logger;
import hudson.plugins.sonar.utils.MaskPasswordsOutputStream;
import hudson.plugins.sonar.utils.SQServerVersions;
import hudson.plugins.sonar.utils.SonarUtils;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.ArgumentListBuilder;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

public class SonarBuildWrapper extends BuildWrapper {
  private static final String DEFAULT_SONAR = "sonar";
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
  public OutputStream decorateLogger(AbstractBuild build, OutputStream outputStream) throws IOException, InterruptedException {
    SonarInstallation inst = SonarInstallation.get(getInstallationName());
    if (inst == null) {
      return outputStream;
    }

    Logger.LOG.info(Messages.SonarBuildWrapper_MaskingPasswords());

    List<String> passwords = new ArrayList<>();

    if (!StringUtils.isBlank(inst.getDatabasePassword())) {
      passwords.add(inst.getDatabasePassword());
    }
    if (!StringUtils.isBlank(inst.getSonarPassword())) {
      passwords.add(inst.getSonarPassword());
    }
    if (!StringUtils.isBlank(inst.getServerAuthenticationToken())) {
      passwords.add(inst.getServerAuthenticationToken());
    }

    return new MaskPasswordsOutputStream(outputStream, build.getCharset(), passwords);
  }

  @Override
  public Collection<? extends Action> getProjectActions(AbstractProject job) {
    return Collections.singletonList(new SonarMarkerAction());
  }

  /**
   * @return name of {@link hudson.plugins.sonar.SonarInstallation}
   */
  @Nullable
  public String getInstallationName() {
    return installationName;
  }

  public void setInstallationName(@Nullable String installationName) {
    this.installationName = installationName;
  }

  @Override
  public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
    if (!SonarInstallation.isValid(getInstallationName(), listener)) {
      return new Environment() {
      };
    }

    return new SonarEnvironment(SonarInstallation.get(getInstallationName()), listener.getLogger());
  }

  @Extension
  public static final class DescriptorImpl extends BuildWrapperDescriptor {
    @Override
    public String getDisplayName() {
      return Messages.SonarBuildWrapper_DisplayName();
    }

    @Override
    public boolean isApplicable(AbstractProject<?, ?> item) {
      return Jenkins.getInstance().getDescriptorByType(SonarGlobalConfiguration.class).isBuildWrapperEnabled();
    }

    /**
     * @return all configured {@link hudson.plugins.sonar.SonarInstallation}
     */
    public SonarInstallation[] getSonarInstallations() {
      return SonarInstallation.all();
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
      String msg = Messages.SonarBuildWrapper_Injecting(installation.getName());
      Logger.LOG.info(msg);
      buildLogger.println(msg);

      Map<String, String> sonarEnv = createVars(installation);

      // resolve variables against each other
      Map<String, String> sonarEnvResolved = new HashMap<>(sonarEnv);
      EnvVars.resolve(sonarEnvResolved);

      for (String k : sonarEnv.keySet()) {
        String v = sonarEnvResolved.get(k);
        env.put(k, v);
      }
    }

    @Override
    public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
      // null result means success so far. If no logs are found, it's probably because it was simply skipped
      if (build.getResult() == null) {
        SonarUtils.addBuildInfoTo(build, installation.getName(), true);
      } else {
        SonarUtils.addBuildInfoTo(build, installation.getName(), false);
      }

      return true;
    }

    private Map<String, String> createVars(SonarInstallation inst) {
      Map<String, String> map = new HashMap<>();

      map.put("SONAR_CONFIG_NAME", inst.getName());
      map.put("SONAR_HOST_URL", getOrDefault(inst.getServerUrl(), "http://localhost:9000"));
      map.put("SONAR_LOGIN", getOrDefault(inst.getSonarLogin(), ""));
      map.put("SONAR_PASSWORD", getOrDefault(inst.getSonarPassword(), ""));
      map.put("SONAR_AUTH_TOKEN", getOrDefault(inst.getServerAuthenticationToken(), ""));
      map.put("SONAR_JDBC_URL", getOrDefault(inst.getDatabaseUrl(), ""));

      String jdbcDefault = SQServerVersions.SQ_5_1_OR_LOWER.equals(inst.getServerVersion()) ? DEFAULT_SONAR : "";
      map.put("SONAR_JDBC_USERNAME", getOrDefault(inst.getDatabaseLogin(), jdbcDefault));
      map.put("SONAR_JDBC_PASSWORD", getOrDefault(inst.getDatabasePassword(), jdbcDefault));

      if (StringUtils.isEmpty(inst.getMojoVersion())) {
        map.put("SONAR_MAVEN_GOAL", "sonar:sonar");
      } else {
        map.put("SONAR_MAVEN_GOAL", SonarUtils.getMavenGoal(inst.getMojoVersion()));
      }

      map.put("SONAR_EXTRA_PROPS", getOrDefault(getAdditionalProps(inst), ""));

      return map;
    }

    private String getAdditionalProps(SonarInstallation inst) {
      ArgumentListBuilder builder = new ArgumentListBuilder();
      // no need to tokenize since we need a String anyway
      builder.add(inst.getAdditionalAnalysisPropertiesUnix());
      builder.add(inst.getAdditionalProperties());

      return StringUtils.join(builder.toList(), ' ');
    }

    private String getOrDefault(String value, String defaultValue) {
      return !StringUtils.isEmpty(value) ? value : defaultValue;
    }
  }
}
