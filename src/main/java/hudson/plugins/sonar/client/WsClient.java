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

public class WsClient {
  public static final String API_RESOURCES = "/api/resources?format=json&depth=0&metrics=alert_status&resource=";
  public static final String API_MEASURES = "/api/measures/component?metricKeys=alert_status&componentKey=";
  public static final String API_PROJECT_STATUS = "/api/qualitygates/project_status?projectKey=";
  public static final String API_VERSION = "/api/server/version";
  public static final String API_PROJECT_NAME = "/api/projects/index?format=json&key=";
  public static final String API_CE_TASK = "/api/ce/task?id=";

  private final HttpClient client;
  private final String serverUrl;
  private final String username;
  private final String password;

  public WsClient(HttpClient client, String serverUrl, String username, String password) {
    this.client = client;
    this.serverUrl = serverUrl;
    this.username = username;
    this.password = password;
  }

  public CETask getCETask(String taskId) throws Exception {
    String url = serverUrl + API_CE_TASK + taskId;
    String text = client.getHttp(url, username, password);
    JSONObject json = (JSONObject) JSONSerializer.toJSON(text);
    JSONObject task = json.getJSONObject("task");

    String status = task.getString("status");
    String componentName = task.getString("componentName");
    String componentKey = task.getString("componentKey");

    return new CETask(status, componentName, componentKey, url);
  }

  public ProjectQualityGate getQualityGate54(String projectKey) throws Exception {
    String url = serverUrl + API_PROJECT_STATUS + encode(projectKey);
    String text = client.getHttp(url, username, password);
    JSONObject json = (JSONObject) JSONSerializer.toJSON(text);
    JSONObject projectStatus = json.getJSONObject("projectStatus");

    String status = projectStatus.getString("status");

    return new ProjectQualityGate(null, status);
  }

  @CheckForNull
  public ProjectQualityGate getQualityGateBefore54(String projectKey) throws Exception {
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

    throw new IllegalStateException("Failed to parse response from resources API: " + url);
  }

  public ProjectQualityGate getQualityGateMeasures(String projectKey) throws Exception {
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

  public String getServerVersion() throws Exception {
    return client.getHttp(serverUrl + API_VERSION, null, null);
  }

  public String getProjectName(String projectKey) throws Exception {
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

  static class CETask {
    private final String status;
    private final String componentName;
    private final String componentKey;
    private final String url;

    public CETask(String status, String componentName, String componentKey, String ceUrl) {
      super();
      this.status = status;
      this.componentName = componentName;
      this.componentKey = componentKey;
      this.url = ceUrl;
    }

    public String getUrl() {
      return url;
    }

    public String getStatus() {
      return status;
    }

    public String getComponentName() {
      return componentName;
    }

    public String getComponentKey() {
      return componentKey;
    }
  }

  static class ProjectQualityGate {
    private final String status;
    private final String projectName;

    ProjectQualityGate(String projectName, String status) {
      this.projectName = projectName;
      this.status = status;
    }

    String getStatus() {
      return status;
    }

    @CheckForNull
    String getProjectName() {
      return projectName;
    }

  }
}
