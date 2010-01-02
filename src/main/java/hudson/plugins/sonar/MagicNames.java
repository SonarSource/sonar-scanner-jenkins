package hudson.plugins.sonar;

/**
 * A simple class to contain strings used by the sonar plugin.
 *
 * @author Evgeny Mandrikov
 */
public final class MagicNames {
  /**
   * Plugin home.
   */
  public static final String PLUGIN_HOME = "/plugin/sonar";

  /**
   * Plugin icon.
   */
  public static final String ICON = PLUGIN_HOME + "/images/sonarsource-wave.png";

  /**
   * Default URL of Sonar.
   */
  public static final String DEFAULT_SONAR_URL = "http://localhost:9000";

  /**
   * Hide utility-class constructor.
   */
  private MagicNames() {
  }
}
