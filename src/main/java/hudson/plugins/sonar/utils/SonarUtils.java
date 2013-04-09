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

import hudson.model.AbstractBuild;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Julien HENRY
 * @since 1.2
 */
public final class SonarUtils {

  /**
   * Pattern for Sonar project URL in logs
   */
  public static final String URL_PATTERN_IN_LOGS = ".*" + Pattern.quote("ANALYSIS SUCCESSFUL, you can browse ") + "(.*)";

  /**
   * Hide utility-class constructor.
   */
  private SonarUtils() {
  }

  public static String extractSonarProjectURLFromLogs(AbstractBuild<?, ?> build) throws IOException {
    BufferedReader br = null;
    String url = null;
    try {
      br = new BufferedReader(build.getLogReader());
      String strLine;
      while ((strLine = br.readLine()) != null) {
        Pattern p = Pattern.compile(URL_PATTERN_IN_LOGS);
        Matcher match = p.matcher(strLine);
        if (match.matches()) {
          url = match.group(1);
        }
      }
    } finally {
      IOUtils.closeQuietly(br);
    }
    return url;
  }
}
