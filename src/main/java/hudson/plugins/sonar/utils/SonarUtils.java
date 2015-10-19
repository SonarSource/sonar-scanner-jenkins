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
package hudson.plugins.sonar.utils;

import hudson.model.AbstractBuild;
import hudson.plugins.sonar.action.UrlSonarAction;
import hudson.model.Run;
import org.apache.commons.io.IOUtils;

import javax.annotation.Nullable;

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

  /**
   * Read logs of the build to find URL of the project dashboard in Sonar
   */
  public static String extractSonarProjectURLFromLogs(Run<?, ?> build) throws IOException {
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

  /**
   * Tries to find a URL in the build logs and appends a @{link UrlSonarAction} to the build. If no URL is found, it
   * tries to get it from the previous build.
   * @return the @{link UrlSonarAction} appended to the build. If no action is appended, null is returned.
   */
  @Nullable
  public static UrlSonarAction addUrlActionTo(Run<?, ?> build) throws IOException {
    UrlSonarAction existingAction = build.getAction(UrlSonarAction.class);
    if (existingAction != null) {
      return existingAction;
    }

    String sonarUrl = extractSonarProjectURLFromLogs(build);
    UrlSonarAction action = null;

    if (sonarUrl == null) {
      Run<?, ?> previousBuild = build.getPreviousBuild();
      if (previousBuild != null) {
        UrlSonarAction previousAction = previousBuild.getAction(UrlSonarAction.class);
        if (previousAction != null) {
          action = new UrlSonarAction(previousAction.getSonarUrl(), false);
          build.addAction(action);
        }
      }
    } else {
      action = new UrlSonarAction(sonarUrl, true);
      build.addAction(action);
    }

    return action;
  }

  public static String getSonarUrlFrom(@Nullable AbstractBuild<?, ?> build) {
    if(build == null) {
      return null;
    }
    
    UrlSonarAction action = build.getAction(UrlSonarAction.class);
    if (action != null) {
      return action.getSonarUrl();
    }

    return null;
  }
}
