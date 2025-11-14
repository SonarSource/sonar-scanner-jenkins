/*
 * SonarQube Scanner for Jenkins
 * Copyright (C) 2007-2025 SonarSource Sàrl
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

import hudson.plugins.sonar.client.WsClient.CETask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WsClientTest {
  private static final String SERVER_URL = "http://myserver.org";
  private static final String TOKEN = "token";
  private static final String TASK_ID = "AVL5i1TZIrFAZSZNbMcg";

  private final HttpClient client = mock(HttpClient.class);
  private WsClient wsClient;

  @BeforeEach
  void setUp() {
    wsClient = new WsClient(client, SERVER_URL, TOKEN);
  }

  @Test
  void testCETask() throws Exception {
    String ws = "/api/ce/task?id=" + TASK_ID;
    String json = getFile("ce_task.json");
    mockWs(ws, json);

    CETask ceTask = wsClient.getCETask(TASK_ID);

    assertThat(ceTask.getComponentKey()).isEqualTo("org.sonarsource.sonarlint:sonarlint-cli");
    assertThat(ceTask.getComponentName()).isEqualTo("SonarLint CLI");
    assertThat(ceTask.getStatus()).isEqualTo("SUCCESS");
    verifyWs(ws);
  }

  @Test
  void testGetVersion() {
    setSQVersion(5.1f);

    assertThat(wsClient.getServerVersion()).isEqualTo("5.1");
    verify(client).getHttp(SERVER_URL + "/api/server/version", null);
  }

  @Test
  void testConnectionError() {
    assertThrows(Exception.class, () -> {
      when(client.getHttp(anyString(), anyString())).thenThrow(RuntimeException.class);
      wsClient.getCETask(TASK_ID);
    });
  }

  @Test
  void testUrlFormat() {
    new WsClient(client, "http://url.com/", null).getServerVersion();
    verify(client).getHttp("http://url.com" + WsClient.API_VERSION, null);
  }

  private void setSQVersion(float version) {
    when(client.getHttp(SERVER_URL + "/api/server/version", null)).thenReturn(Float.toString(version));
  }

  private void verifyWs(String ws) {
    verify(client).getHttp(SERVER_URL + ws, TOKEN);
  }

  private void mockWs(String ws, String response) {
    when(client.getHttp(eq(SERVER_URL + ws), anyString())).thenReturn(response);
  }

  private String getFile(String name) throws Exception {
    URL resource = this.getClass().getResource(name);
    Path p = Paths.get(resource.toURI());
    return Files.readString(p);
  }

}
