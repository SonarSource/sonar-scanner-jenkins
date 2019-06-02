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
package hudson.plugins.sonar.action;

import hudson.model.InvisibleAction;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * Persists in a build SonarQube related information.
 */
@ExportedBean(defaultVisibility = 2)
public class SonarAnalysisAction extends InvisibleAction {
  private String installationName;
  private String credentialsId;
  private String ceTaskId;
  // Dashboard URL
  private String url;
  private String serverUrl;
  private boolean isNew;
  private boolean isSkipped;

  public SonarAnalysisAction(String installationName, @Nullable String credentialId) {
    this.installationName = installationName;
    this.credentialsId = credentialId;
    this.url = null;
    this.ceTaskId = null;
    this.isNew = true;
    this.isSkipped = false;
    this.serverUrl = null;
  }

  public SonarAnalysisAction(SonarAnalysisAction copy) {
    this.installationName = copy.installationName;
    this.credentialsId = copy.credentialsId;
    this.url = copy.url;
    this.serverUrl = copy.serverUrl;
    this.ceTaskId = null;
    this.isNew = false;
    this.isSkipped = false;
  }

  public void setNew(boolean isNew) {
    this.isNew = isNew;
  }

  @CheckForNull
  @Exported
  public String getCeTaskId() {
    return ceTaskId;
  }

  public void setCeTaskId(String ceTaskId) {
    this.ceTaskId = ceTaskId;
  }

  @CheckForNull
  @Exported
  public String getServerUrl() {
    return serverUrl;
  }

  public void setServerUrl(String serverUrl) {
    this.serverUrl = serverUrl;
  }

  @Exported
  public boolean isNew() {
    return isNew;
  }

  public void setSkipped(boolean isSkipped) {
    this.isSkipped = isSkipped;
  }

  @Exported
  public boolean isSkipped() {
    return isSkipped;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  @CheckForNull
  @Exported(name = "sonarqubeDashboardUrl")
  public String getUrl() {
    return url;
  }

  @Exported
  public String getInstallationName() {
    return installationName;
  }

  @CheckForNull
  @Exported
  public String getCredentialsId() {
    return credentialsId;
  }
}
