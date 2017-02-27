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

import hudson.plugins.sonar.SonarInstallation;
import hudson.plugins.sonar.utils.Logger;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.commons.lang.StringUtils;

public class WsClient {
  private static final String STATUS_ATTR = "status";
  public static final String API_RESOURCES = "/api/resources?format=json&depth=0&metrics=alert_status&resource=";
  public static final String API_MEASURES = "/api/measures/component?metricKeys=alert_status&componentKey=";
  public static final String API_PROJECT_STATUS = "/api/qualitygates/project_status?projectKey=";
  public static final String API_PROJECT_STATUS_WITH_ANALYSISID = "/api/qualitygates/project_status?analysisId=";
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

  public static WsClient create(HttpClient client, SonarInstallation inst) {
    String serverUrl = StringUtils.isEmpty(inst.getServerUrl()) ? SonarInstallation.DEFAULT_SERVER_URL : inst.getServerUrl();
    if (StringUtils.isNotEmpty(inst.getServerAuthenticationToken())) {
      return new WsClient(client, serverUrl, inst.getServerAuthenticationToken(), null);
    } else {
      return new WsClient(client, serverUrl, inst.getSonarLogin(), inst.getSonarPassword());
    }
  }

  public CETask getCETask(String taskId) {
    String url = serverUrl + API_CE_TASK + taskId;
    String text = client.getHttp(url, username, password);
    try {
      JSONObject json = (JSONObject) JSONSerializer.toJSON(text);
      JSONObject task = json.getJSONObject("task");

      String status = task.getString(STATUS_ATTR);
      String componentName = task.getString("componentName");
      String componentKey = task.getString("componentKey");
      // No analysisId if task is pending
      String analysisId = task.optString("analysisId", null);
      return new CETask(status, componentName, componentKey, url, analysisId);
    } catch (JSONException e) {
      throw new IllegalStateException("Unable to parse response from " + url + ":\n" + text, e);
    }

  }

  public ProjectQualityGate getQualityGateWithAnalysisId(String analysisId) {
    String url = serverUrl + API_PROJECT_STATUS_WITH_ANALYSISID + encode(analysisId);
    String text = client.getHttp(url, username, password);
    try {
      JSONObject json = (JSONObject) JSONSerializer.toJSON(text);
      JSONObject projectStatus = json.getJSONObject("projectStatus");

      String status = projectStatus.getString(STATUS_ATTR);

      return new ProjectQualityGate(null, status);
    } catch (JSONException e) {
      throw new IllegalStateException("Unable to parse response from " + url + ":\n" + text, e);
    }
  }

  public ProjectQualityGate getQualityGate54(String projectKey) throws Exception {
    String url = serverUrl + API_PROJECT_STATUS + encode(projectKey);
    String text = client.getHttp(url, username, password);
    JSONObject json = (JSONObject) JSONSerializer.toJSON(text);
    JSONObject projectStatus = json.getJSONObject("projectStatus");

    String status = projectStatus.getString(STATUS_ATTR);

    return new ProjectQualityGate(null, status);
  }

  @CheckForNull
  public ProjectQualityGate getQualityGateBefore54(String projectKey) {
    String url = serverUrl + API_RESOURCES + encode(projectKey);
    String text = client.getHttp(url, username, password);
    JSONArray resourceArray = (JSONArray) JSONSerializer.toJSON(text);

    if (resourceArray.size() != 1) {
      Logger.LOG.fine(() -> "Found " + resourceArray.size() + " resources for " + projectKey);
      // in 4.5, for example, there is no default QG and a project might return an empty array.
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

  public ProjectQualityGate getQualityGateMeasures(String projectKey) throws MessageException {
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

  public String getServerVersion() {
    return client.getHttp(serverUrl + API_VERSION, null, null);
  }

  public String getProjectName(String projectKey) {
    String url = serverUrl + API_PROJECT_NAME + encode(projectKey);
    String http = client.getHttp(url, username, password);
    JSONArray jsonArray = (JSONArray) JSONSerializer.toJSON(http);
    if (jsonArray.size() != 1) {
      throw new IllegalStateException("Can't find project " + projectKey + ". Number of projects found: " + jsonArray.size());
    }

    JSONObject obj = jsonArray.getJSONObject(0);
    return obj.getString("nm");
  }

  private static String encode(String param) {
    try {
      return URLEncoder.encode(param, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      // Should never occurs
      return param;
    }
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

  public static class CETask {

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILURE = "FAILED";
    public static final String STATUS_CANCELED = "CANCELED";

    private final String status;
    private final String componentName;
    private final String componentKey;
    private final String url;
    private final String analysisId;

    public CETask(String status, String componentName, String componentKey, String ceUrl, @Nullable String analysisId) {
      this.status = status;
      this.componentName = componentName;
      this.componentKey = componentKey;
      this.url = ceUrl;
      this.analysisId = analysisId;
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

    /**
     * @return null if status is PENDING
     */
    @CheckForNull
    public String getAnalysisId() {
      return analysisId;
    }
  }

  public static class ProjectQualityGate {
    private final String status;
    private final String projectName;

    ProjectQualityGate(String projectName, String status) {
      this.projectName = projectName;
      this.status = status;
    }

    public String getStatus() {
      return status;
    }

    @CheckForNull
    public String getProjectName() {
      return projectName;
    }

  }
}
