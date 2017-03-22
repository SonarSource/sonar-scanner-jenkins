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
package hudson.plugins.sonar.utils;

import hudson.FilePath;
import hudson.model.Action;
import hudson.model.Actionable;
import hudson.model.Run;
import hudson.plugins.sonar.action.SonarAnalysisAction;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * @author Julien HENRY
 * @since 1.2
 */
public final class SonarUtils {

  /**
   * Pattern for Sonar project URL in logs
   */
  public static final String URL_PATTERN_IN_LOGS = ".*" + Pattern.quote("ANALYSIS SUCCESSFUL, you can browse ") + "(.*)";
  public static final String WORKING_DIR_PATTERN_IN_LOGS = ".*" + Pattern.quote("Working dir: ") + "(.*)";
  public static final String DASHBOARD_URL_KEY = "dashboardUrl";
  public static final String CE_TASK_ID_KEY = "ceTaskId";
  public static final String REPORT_TASK_FILE_NAME = "report-task.txt";

  /**
   * Hide utility-class constructor.
   */
  private SonarUtils() {
  }

  /**
   * Read logs of the build to find URL of the project dashboard in Sonar
   */
  public static String extractSonarProjectURLFromLogs(Run<?, ?> build) throws IOException {
    return extractPatternFromLogs(URL_PATTERN_IN_LOGS, build);
  }

  public static <T extends Action> List<T> getPersistentActions(Actionable actionable, Class<T> type) {
    List<T> filtered = new LinkedList<>();

    // we use this method to avoid recursively calling transitive action factories
    for (Action a : actionable.getActions()) {
      if (a == null) {
        continue;
      }
      if (type.isAssignableFrom(a.getClass())) {
        filtered.add((T) a);
      }
    }
    return filtered;
  }

  @CheckForNull
  public static <T extends Action> T getPersistentAction(Actionable actionable, Class<T> type) {
    // we use this method to avoid recursively calling transitive action factories
    for (Action a : actionable.getActions()) {
      if (a == null) {
        continue;
      }
      if (type.isAssignableFrom(a.getClass())) {
        return (T) a;
      }
    }
    return null;
  }

  public static Properties extractReportTask(Run<?, ?> build, FilePath workspace) throws IOException, InterruptedException {
    String workDirPath = extractPatternFromLogs(WORKING_DIR_PATTERN_IN_LOGS, build);
    if (workDirPath == null) {
      return null;
    }

    FilePath workDir = workspace.child(workDirPath);

    FilePath reportTaskFile = workDir.child(REPORT_TASK_FILE_NAME);
    if (!reportTaskFile.exists()) {
      return null;
    }

    String content = reportTaskFile.readToString();
    Properties p = new Properties();
    p.load(new StringReader(content));
    return p;

  }

  private static String extractPatternFromLogs(String pattern, Run<?, ?> build) throws IOException {
    String url = null;
    try (BufferedReader br = new BufferedReader(build.getLogReader())) {
      String strLine;
      Pattern p = Pattern.compile(pattern);
      while ((strLine = br.readLine()) != null) {
        Matcher match = p.matcher(strLine);
        if (match.matches()) {
          url = match.group(1);
        }
      }
    }
    return url;
  }

  @Nullable
  /** 
   * Collects as much information as it finds from the sonar analysis in the build and adds it as an action to the build.
   * Even if no information is found, the action is added, marking in the build that a sonar analysis ran. 
   */
  public static SonarAnalysisAction addBuildInfoTo(Run<?, ?> build, FilePath workspace, String installationName, boolean skippedIfNoBuild)
    throws IOException, InterruptedException {
    SonarAnalysisAction buildInfo = new SonarAnalysisAction(installationName);
    Properties reportTask = extractReportTask(build, workspace);

    if (reportTask != null) {
      buildInfo.setUrl(reportTask.getProperty(DASHBOARD_URL_KEY));
      buildInfo.setCeTaskId(reportTask.getProperty(CE_TASK_ID_KEY));
    } else {
      String sonarUrl = extractSonarProjectURLFromLogs(build);
      if (sonarUrl == null) {
        return addBuildInfoFromLastBuildTo(build, installationName, skippedIfNoBuild);
      }
      buildInfo.setUrl(sonarUrl);
    }

    build.addAction(buildInfo);
    return buildInfo;
  }

  public static SonarAnalysisAction addBuildInfoTo(Run<?, ?> build, FilePath workspace, String installationName) throws IOException, InterruptedException {
    return addBuildInfoTo(build, workspace, installationName, false);
  }

  public static SonarAnalysisAction addBuildInfoFromLastBuildTo(Run<?, ?> build, String installationName, boolean isSkipped) {
    Run<?, ?> previousBuild = build.getPreviousBuild();
    if (previousBuild == null) {
      return addEmptyBuildInfo(build, installationName, isSkipped);
    }

    for (SonarAnalysisAction analysis : previousBuild.getActions(SonarAnalysisAction.class)) {
      if (analysis.getUrl() != null && analysis.getInstallationName().equals(installationName)) {
        SonarAnalysisAction copy = new SonarAnalysisAction(analysis);
        copy.setSkipped(isSkipped);
        build.addAction(copy);
        return copy;
      }
    }
    return addEmptyBuildInfo(build, installationName, isSkipped);
  }

  public static SonarAnalysisAction addEmptyBuildInfo(Run<?, ?> build, String installationName, boolean isSkipped) {
    SonarAnalysisAction analysis = new SonarAnalysisAction(installationName);
    analysis.setSkipped(isSkipped);
    build.addAction(analysis);
    return analysis;
  }

  public static String getMavenGoal(String version) {
    Float majorMinor = extractMajorMinor(version);

    if (majorMinor == null || majorMinor >= 3.0) {
      return "org.sonarsource.scanner.maven:sonar-maven-plugin:" + version + ":sonar";
    } else {
      return "org.codehaus.mojo:sonar-maven-plugin:" + version + ":sonar";
    }
  }

  @CheckForNull
  public static Float extractMajorMinor(String version) {
    Pattern p = Pattern.compile("\\d+\\.\\d+");
    Matcher m = p.matcher(version);

    if (m.find()) {
      return Float.parseFloat(m.group());
    }

    return null;
  }
}
