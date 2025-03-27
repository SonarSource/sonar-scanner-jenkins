/*
 * SonarQube Scanner for Jenkins
 * Copyright (C) 2007-2025 SonarSource SA
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
package org.sonarsource.scanner.jenkins.pipeline;

import com.google.common.annotations.VisibleForTesting;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import hudson.Extension;
import hudson.model.RootAction;
import hudson.model.UnprotectedRootAction;
import hudson.plugins.sonar.client.WsClient.CETask;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import edu.umd.cs.findbugs.annotations.Nullable;
import jakarta.servlet.http.HttpServletResponse;
import jenkins.model.Jenkins;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.interceptor.RequirePOST;

@Extension
public class SonarQubeWebHook implements UnprotectedRootAction {
  private static final Logger LOGGER = Logger.getLogger(SonarQubeWebHook.class.getName());
  private final Cache<String, WebhookEvent> eventCache = Caffeine.newBuilder().expireAfterWrite(2, TimeUnit.HOURS).build();
  public static final String URLNAME = "sonarqube-webhook";

  @VisibleForTesting
  List<Consumer<WebhookEvent>> listeners = new CopyOnWriteArrayList<>();

  @Override
  public String getIconFileName() {
    return null;
  }

  @Override
  public String getDisplayName() {
    return null;
  }

  @Override
  public String getUrlName() {
    return URLNAME;
  }

  @RequirePOST
  public void doIndex(StaplerRequest2 req, StaplerResponse2 rsp) throws IOException {
    String payload = IOUtils.toString(req.getReader());

    LOGGER.info("Received POST from " + req.getRemoteHost());
    try {
      JSONObject jsonObject = validate(payload);
      LOGGER.fine(() -> "Full details of the POST was " + jsonObject.toString());

      WebhookEvent event = new WebhookEvent(new Payload(payload, jsonObject), req.getHeader("X-Sonar-Webhook-HMAC-SHA256"));

      eventCache.put(event.payload.taskId, event);

      for (Consumer<WebhookEvent> listener : listeners) {
        listener.accept(event);
      }
    } catch (JSONException e) {
      LOGGER.log(Level.WARNING, e, () -> "Invalid payload " + payload);
      rsp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON Payload");
    }
    rsp.setStatus(HttpServletResponse.SC_OK);
  }

  private static JSONObject validate(String payload) {
    return JSONObject.fromObject(payload);
  }

  public static SonarQubeWebHook get() {
    return Jenkins.get().getExtensionList(RootAction.class).get(SonarQubeWebHook.class);
  }

  public void addListener(Consumer<WebhookEvent> l) {
    listeners.add(l);
  }

  public void removeListener(Consumer<WebhookEvent> l) {
    listeners.remove(l);
  }

  @Nullable
  public WebhookEvent getWebhookEventForTaskId(String taskId) {
    return eventCache.getIfPresent(taskId);
  }

  static final class WebhookEvent {
    private final Payload payload;
    private final String receivedSignature;

    WebhookEvent(Payload payload, String receivedSignature) {
      this.payload = payload;
      this.receivedSignature = receivedSignature;
    }

    public Payload getPayload() {
      return payload;
    }

    public String getReceivedSignature() {
      return receivedSignature;
    }
  }

  static final class Payload {

    private final String payloadAsString;
    private final String taskId;
    private final String componentName;
    private final String taskStatus;
    private final String qualityGateStatus;
    private final String dashboardUrl;

    Payload(String payloadAsString, JSONObject json) {
      this.payloadAsString = payloadAsString;
      this.taskId = json.getString("taskId");
      this.taskStatus = json.getString("status");
      JSONObject project = json.getJSONObject("project");
      this.componentName = project.getString("name");
      this.dashboardUrl = project.getString("url");
      if (CETask.STATUS_SUCCESS.equals(getTaskStatus())) {
        this.qualityGateStatus = json.has("qualityGate") ? json.getJSONObject("qualityGate").getString("status") : "NONE";
      } else {
        this.qualityGateStatus = null;
      }
    }

    String getTaskId() {
      return taskId;
    }

    String getTaskStatus() {
      return taskStatus;
    }

    String getQualityGateStatus() {
      return qualityGateStatus;
    }

    String getComponentName() {
      return componentName;
    }

    String getDashboardUrl() {
      return dashboardUrl;
    }

    String getPayloadAsString() {
      return payloadAsString;
    }

  }

}
