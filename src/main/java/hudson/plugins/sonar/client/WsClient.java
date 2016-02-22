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

import hudson.plugins.sonar.utils.Logger;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import javax.annotation.CheckForNull;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;

public class WsClient {
  public static final String API_RESOURCES = "/api/resources?format=json&depth=0&metrics=alert_status&resource=";
  public static final String API_MEASURES = "/api/measures/component?metricKeys=alert_status&componentKey=";
  public static final String API_PROJECT_STATUS = "/api/qualitygates/project_status?projectKey=";
  public static final String API_VERSION = "/api/server/version";
  public static final String API_PROJECT_NAME = "/api/projects/index?format=json&key=";

  private final HttpClient client;
  private final String serverUrl;
  private final String username;
  private final String password;
  private final String projectKey;

  public WsClient(HttpClient client, String serverUrl, String projectKey, String username, String password) {
    this.client = client;
    this.serverUrl = serverUrl;
    this.projectKey = projectKey;
    this.username = username;
    this.password = password;
  }

  public ProjectQualityGate getQualityGate52() throws Exception {
    String url = serverUrl + API_PROJECT_STATUS + encode(projectKey);
    String text = client.getHttp(url, username, password);
    JSONObject json = (JSONObject) JSONSerializer.toJSON(text);
    JSONObject projectStatus = json.getJSONObject("projectStatus");

    String status = projectStatus.getString("status");
    String projectName = getProjectName();

    return new ProjectQualityGate(projectName, status);
  }

  public ProjectQualityGate getQualityGateBefore52() throws Exception {
    String url = serverUrl + API_RESOURCES + encode(projectKey);

    String text = client.getHttp(url, username, password);
    JSONArray resourceArray = (JSONArray) JSONSerializer.toJSON(text);

    if (resourceArray.size() != 1) {
      Logger.LOG.fine("Found " + resourceArray.size() + " resources for " + projectKey);
      return null;
    }

    JSONObject resource = resourceArray.getJSONObject(0);

    String projectName = resource.getString("name");

    JSONArray measuresArray = resource.getJSONArray("msr");
    for (Object o : measuresArray) {
      JSONObject jsonObj = (JSONObject) o;
      String key = jsonObj.getString("key");

      if ("alert_status".equals(key)) {
        return new ProjectQualityGate(projectName, jsonObj.getString("data"));
      }
    }

    return null;
  }

  public ProjectQualityGate getQualityGateMeasures() throws Exception {
    String url = serverUrl + API_MEASURES + encode(projectKey);

    String text = client.getHttp(url, username, password);
    JSONObject json = (JSONObject) JSONSerializer.toJSON(text);
    if (json.containsKey("errors")) {
      throw new MessageException(parseError(json));
    }

    JSONObject component = json.getJSONObject("component");

    String projectName = component.getString("name");

    JSONArray measuresArray = component.getJSONArray("measures");
    for (Object o : measuresArray) {
      JSONObject jsonObj = (JSONObject) o;
      String metric = jsonObj.getString("metric");

      if ("alert_status".equals(metric)) {
        return new ProjectQualityGate(projectName, jsonObj.getString("value"));
      }
    }

    return null;
  }

  public String getServerVersion(String serverUrl) throws Exception {
    return client.getHttp(serverUrl + API_VERSION, null, null);
  }

  private String getProjectName() throws Exception {
    String url = serverUrl + API_PROJECT_NAME + encode(projectKey);

    String http = client.getHttp(url, username, password);
    JSONArray jsonArray = (JSONArray) JSONSerializer.toJSON(http);
    if (jsonArray.size() != 1) {
      throw new Exception("Can't find project " + projectKey + ". Number of projects found: " + jsonArray.size());
    }

    JSONObject obj = jsonArray.getJSONObject(0);
    return obj.getString("nm");
  }

  private static String encode(String param) throws UnsupportedEncodingException {
    return URLEncoder.encode(param, "UTF-8");
  }

  private static String parseError(JSONObject json) {
    JSONArray array = json.getJSONArray("errors");
    StringBuilder errorMsg = new StringBuilder();

    for (int i = 0; i < array.size(); i++) {
      JSONObject obj = array.getJSONObject(i);
      errorMsg.append(obj.getString("msg")).append("<br/>");
    }

    return errorMsg.toString();
  }

  static class ProjectQualityGate {
    private final String status;
    private final String projectName;
    private final Date date;

    ProjectQualityGate(String projectName, String status) {
      this.projectName = projectName;
      this.status = status;
      this.date = null;
    }

    @CheckForNull
    Date getDate() {
      return date;
    }

    String getStatus() {
      return status;
    }

    String getProjectName() {
      return projectName;
    }

  }
}
