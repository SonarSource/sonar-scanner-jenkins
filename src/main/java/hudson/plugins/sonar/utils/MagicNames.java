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
package hudson.plugins.sonar.utils;

/**
 * A simple class to contain strings used by the sonar plugin.
 *
 * @author Evgeny Mandrikov
 * @since 1.2
 */
public final class MagicNames {
  /**
   * Plugin home.
   */
  public static final String PLUGIN_HOME = "/plugin/sonar";

  /**
   * Plugin icon.
   */
  public static final String ICON = PLUGIN_HOME + "/images/sonar-wave.png";

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
