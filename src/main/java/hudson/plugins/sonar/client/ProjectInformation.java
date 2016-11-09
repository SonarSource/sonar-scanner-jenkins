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
package hudson.plugins.sonar.client;

import hudson.model.InvisibleAction;
import java.util.Locale;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class ProjectInformation extends InvisibleAction {
  private long created;
  private String[] errors;
  private String key;
  private String name;
  private String url;
  private String status;
  private String ceStatus;
  private String ceUrl;

  public ProjectInformation(String projectKey) {
    this.key = projectKey;
    this.created = System.currentTimeMillis();
  }

  public long created() {
    return created;
  }

  public String getCeUrl() {
    return ceUrl;
  }

  public void setCeUrl(String ceUrl) {
    this.ceUrl = ceUrl;
  }

  public String getCeStatus() {
    return ceStatus;
  }

  public void setCeStatus(@Nullable String ceStatus) {
    this.ceStatus = (ceStatus != null) ? ceStatus.toLowerCase(Locale.US) : null;
  }

  @CheckForNull
  public String getStatus() {
    return status;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getProjectKey() {
    return key;
  }

  public String getProjectName() {
    return name;
  }

  public boolean hasErrors() {
    return errors != null && errors.length > 0;
  }

  public String[] getErrors() {
    return errors;
  }

  public void setErrors(String[] errors) {
    this.errors = errors;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
