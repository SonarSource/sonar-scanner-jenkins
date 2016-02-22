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
package hudson.plugins.sonar.action;

import hudson.model.Action;
import hudson.model.InvisibleAction;

import java.util.Properties;

/**
 * Persists in a build SonarQube related information.
 */
public class SonarAnalysisAction extends InvisibleAction implements Action {
  private String installationName;
  private Properties reportTask;
  private String url;
  private boolean isNew;
  private boolean isSkipped;

  public SonarAnalysisAction(String installationName, Properties reportTask) {
    this.installationName = installationName;
    this.reportTask = reportTask;
    this.isNew = true;
    this.isSkipped = false;
  }

  public SonarAnalysisAction(SonarAnalysisAction copy) {
    this.installationName = copy.installationName;
    this.reportTask = copy.reportTask;
    this.url = copy.url;
    this.isNew = false;
    this.isSkipped = false;
  }

  public SonarAnalysisAction(String installationName) {
    this.installationName = installationName;
    this.isNew = true;
  }

  public void setNew(boolean isNew) {
    this.isNew = isNew;
  }

  public boolean isNew() {
    return isNew;
  }

  public void setSkipped(boolean isSkipped) {
    this.isSkipped = isSkipped;
  }

  public boolean isSkipped() {
    return isSkipped;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public void setReportTask(Properties reportTask) {
    this.reportTask = reportTask;
  }

  public String getUrl() {
    return url;
  }

  public String getInstallationName() {
    return installationName;
  }
}
