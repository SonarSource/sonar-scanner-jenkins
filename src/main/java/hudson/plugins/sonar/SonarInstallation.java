package hudson.plugins.sonar;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

public final class SonarInstallation {
  private final String name;
  private final boolean disabled;
  private final String serverUrl;
  private final String databaseUrl;
  private final String databaseDriver;
  private final String databaseLogin;
  private final String databasePassword;
  private final String additionalProperties;

  @DataBoundConstructor
  public SonarInstallation(String name, boolean disabled, String serverUrl, String databaseUrl,
                           String databaseDriver, String databaseLogin, String databasePassword, String additionalProperties) {
    this.name = name;
    this.disabled = disabled;
    this.serverUrl = serverUrl;
    this.databaseUrl = databaseUrl;
    this.databaseDriver = databaseDriver;
    this.databaseLogin = databaseLogin;
    this.databasePassword = databasePassword;
    this.additionalProperties = additionalProperties;
  }

  public String getName() {
    return name;
  }

  public boolean isDisabled() {
    return disabled;
  }

  public String getServerUrl() {
    return serverUrl;
  }

  public String getDatabaseUrl() {
    return databaseUrl;
  }

  public String getDatabaseDriver() {
    return databaseDriver;
  }

  public String getDatabaseLogin() {
    return databaseLogin;
  }

  public String getDatabasePassword() {
    return databasePassword;
  }

  public String getAdditionalProperties() {
    return additionalProperties;
  }

  public String getPluginCallArgs() {
    StringBuilder builder = new StringBuilder(100);
    builder.append("sonar:sonar");
    appendUnlessEmpty(builder, "sonar.jdbc.driver", databaseDriver);
    appendUnlessEmpty(builder, "sonar.jdbc.username", databaseLogin);
    appendUnlessEmpty(builder, "sonar.jdbc.password", databasePassword);
    appendUnlessEmpty(builder, "sonar.jdbc.url", databaseUrl);
    appendUnlessEmpty(builder, "sonar.host.url", serverUrl);
    appendUnlessEmpty(builder, "sonar.skipInstall", "true");
    if (StringUtils.isNotBlank(additionalProperties)) {
      builder.append(' ');
      builder.append(additionalProperties);
    }
    return builder.toString();
  }

  private static void appendUnlessEmpty(StringBuilder builder, String key, String value) {
    if (StringUtils.isNotEmpty(StringUtils.defaultString(value))) {
      builder.append(" -D");
      builder.append(key);
      builder.append('=');
      builder.append(value);
    }
  }
}

