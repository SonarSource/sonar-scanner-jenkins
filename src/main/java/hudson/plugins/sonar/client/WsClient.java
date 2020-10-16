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
package hudson.plugins.sonar.client;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

public class WsClient {
  private static final String STATUS_ATTR = "status";
  public static final String API_RESOURCES = "/api/resources?format=json&depth=0&metrics=alert_status&resource=";
  public static final String API_MEASURES = "/api/measures/component?metricKeys=alert_status&componentKey=";
  public static final String API_PROJECT_STATUS_WITH_ANALYSISID = "/api/qualitygates/project_status?analysisId=";
  public static final String API_VERSION = "/api/server/version";
  public static final String API_PROJECT_NAME = "/api/projects/index?format=json&key=";
  public static final String API_CE_TASK = "/api/ce/task?id=";

  private final HttpClient client;
  private final String serverUrl;
  private final String token;

  public WsClient(HttpClient client, String serverUrl, @Nullable String token) {
    this.client = client;
    this.serverUrl = serverUrl;
    this.token = token;
  }

  public CETask getCETask(String taskId) {
    String url = serverUrl + API_CE_TASK + taskId;
    String text = client.getHttp(url, token);
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

  public String requestQualityGateStatus(String analysisId) {
    String url = serverUrl + API_PROJECT_STATUS_WITH_ANALYSISID + encode(analysisId);
    String text = client.getHttp(url, token);
    try {
      JSONObject json = (JSONObject) JSONSerializer.toJSON(text);
      JSONObject projectStatus = json.getJSONObject("projectStatus");

      return projectStatus.getString(STATUS_ATTR);
    } catch (JSONException e) {
      throw new IllegalStateException("Unable to parse response from " + url + ":\n" + text, e);
    }
  }

  public String getServerVersion() {
    return client.getHttp(serverUrl + API_VERSION, null);
  }

  private static String encode(String param) {
    try {
      return URLEncoder.encode(param, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      // Should never occurs
      return param;
    }
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

}
