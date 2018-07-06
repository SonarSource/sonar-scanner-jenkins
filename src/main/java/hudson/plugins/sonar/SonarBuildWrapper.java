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
import hudson.FilePath;
import hudson.Launcher;
import hudson.console.ConsoleLogFilter;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.sonar.action.SonarMarkerAction;
import hudson.plugins.sonar.utils.Logger;
import hudson.plugins.sonar.utils.MaskPasswordsOutputStream;
import hudson.plugins.sonar.utils.SonarUtils;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.ArgumentListBuilder;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import jenkins.tasks.SimpleBuildWrapper;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import static org.apache.commons.lang3.StringEscapeUtils.escapeJson;

public class SonarBuildWrapper extends SimpleBuildWrapper {
  private String installationName = null;

  @DataBoundConstructor
  public SonarBuildWrapper(@Nullable String installationName) {
    this.installationName = installationName;
  }

  @Override
  public void setUp(Context context, Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener, EnvVars initialEnvironment)
    throws IOException, InterruptedException {

    SonarInstallation.checkValid(getInstallationName());
    SonarInstallation installation = SonarInstallation.get(getInstallationName());

    String msg = Messages.SonarBuildWrapper_Injecting(installation.getName());
    Logger.LOG.info(msg);
    listener.getLogger().println(msg);

    context.getEnv().putAll(createVars(installation, initialEnvironment, build));

    context.setDisposer(new AddBuildInfo(installation));

    build.addAction(new SonarMarkerAction());
  }

  @VisibleForTesting
  static Map<String, String> createVars(SonarInstallation inst, EnvVars initialEnvironment, Run<?, ?> build) {
    Map<String, String> map = new HashMap<>();

    map.put("SONAR_CONFIG_NAME", inst.getName());
    String hostUrl = getOrDefault(initialEnvironment.expand(inst.getServerUrl()), "http://localhost:9000");
    map.put("SONAR_HOST_URL", hostUrl);
    String token = getOrDefault(initialEnvironment.expand(inst.getServerAuthenticationToken(build)), "");
    map.put("SONAR_AUTH_TOKEN", token);

    String mojoVersion = inst.getMojoVersion();
    if (StringUtils.isEmpty(mojoVersion)) {
      map.put("SONAR_MAVEN_GOAL", "sonar:sonar");
    } else {
      map.put("SONAR_MAVEN_GOAL", SonarUtils.getMavenGoal(mojoVersion));
    }

    map.put("SONAR_EXTRA_PROPS", getOrDefault(initialEnvironment.expand(getAdditionalProps(inst)), ""));

    // resolve variables against each other
    EnvVars.resolve(map);

    StringBuilder sb = new StringBuilder();
    sb.append("{ \"sonar.host.url\" : \"").append(escapeJson(hostUrl)).append("\"");
    if (!token.isEmpty()) {
      sb.append(", \"sonar.login\" : \"").append(escapeJson(token)).append("\"");
    }
    String additionalAnalysisProperties = inst.getAdditionalAnalysisProperties();
    if (additionalAnalysisProperties != null) {
      for (String pair : StringUtils.split(additionalAnalysisProperties)) {
        String[] keyValue = StringUtils.split(pair, "=");
        if (keyValue.length == 2) {
          sb.append(", \"").append(escapeJson(keyValue[0])).append("\" : \"").append(escapeJson(initialEnvironment.expand(keyValue[1]))).append("\"");
        }
      }
    }
    sb.append("}");

    map.put("SONARQUBE_SCANNER_PARAMS", sb.toString());

    return map;
  }

  private static String getAdditionalProps(SonarInstallation inst) {
    ArgumentListBuilder builder = new ArgumentListBuilder();
    // no need to tokenize since we need a String anyway
    builder.add(inst.getAdditionalAnalysisPropertiesUnix());
    builder.add(inst.getAdditionalProperties());

    return StringUtils.join(builder.toList(), ' ');
  }

  private static String getOrDefault(String value, String defaultValue) {
    return !StringUtils.isEmpty(value) ? value : defaultValue;
  }

  @Override
  public ConsoleLogFilter createLoggerDecorator(Run<?, ?> build) {
    SonarInstallation inst = SonarInstallation.get(getInstallationName());
    if (inst == null) {
      return null;
    }

    Logger.LOG.info(Messages.SonarBuildWrapper_MaskingPasswords());

    List<String> passwords = new ArrayList<>();

    String token = inst.getServerAuthenticationToken(build);
    if (!StringUtils.isBlank(token)) {
      passwords.add(token);
    }

    return new SonarQubePasswordLogFilter(passwords, build.getCharset().name());
  }

  private static final class AddBuildInfo extends Disposer {

    private static final long serialVersionUID = 1L;

    private final SonarInstallation installation;

    public AddBuildInfo(SonarInstallation installation) {
      this.installation = installation;
    }

    @Override
    public void tearDown(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
      // null result means success so far. If no logs are found, it's probably because it was simply skipped
      SonarUtils.addBuildInfoTo(build, listener, workspace, installation.getName(), build.getResult() == null);
    }
  }

  private static class SonarQubePasswordLogFilter extends ConsoleLogFilter implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<String> passwords;
    private final String consoleCharset;

    public SonarQubePasswordLogFilter(List<String> passwords, String consoleCharset) {
      this.passwords = passwords;
      this.consoleCharset = consoleCharset;
    }

    @Override
    public OutputStream decorateLogger(Run ignore, OutputStream logger) throws IOException, InterruptedException {
      return new MaskPasswordsOutputStream(logger, Charset.forName(consoleCharset), passwords);
    }

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

  @Symbol("withSonarQubeEnv")
  @Extension
  public static final class DescriptorImpl extends BuildWrapperDescriptor {
    @Override
    public String getDisplayName() {
      return Messages.SonarBuildWrapper_DisplayName();
    }

    @Override
    public boolean isApplicable(AbstractProject<?, ?> item) {
      return SonarGlobalConfiguration.get().isBuildWrapperEnabled();
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

}
