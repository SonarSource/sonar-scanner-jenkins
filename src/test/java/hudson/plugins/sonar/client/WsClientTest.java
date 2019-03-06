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

import hudson.plugins.sonar.client.WsClient.CETask;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WsClientTest {
  private final static String SERVER_URL = "http://myserver.org";
  private final static String TOKEN = "token";
  private final static String TASK_ID = "AVL5i1TZIrFAZSZNbMcg";

  private HttpClient client = mock(HttpClient.class);
  private WsClient wsClient;

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Before
  public void setUp() {
    wsClient = new WsClient(client, SERVER_URL, TOKEN);
  }

  @Test
  public void testCETask() throws Exception {
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
  public void testGetVersion() throws Exception {
    setSQVersion(5.1f);

    assertThat(wsClient.getServerVersion()).isEqualTo("5.1");
    verify(client).getHttp(SERVER_URL + "/api/server/version", null);
  }

  @Test
  public void testConnectionError() throws Exception {
    when(client.getHttp(anyString(), anyString())).thenThrow(RuntimeException.class);

    exception.expect(Exception.class);
    wsClient.getCETask(TASK_ID);
  }

  private void setSQVersion(float version) throws Exception {
    when(client.getHttp(SERVER_URL + "/api/server/version", null)).thenReturn(Float.toString(version));
  }

  private void verifyWs(String ws) throws Exception {
    verify(client).getHttp(SERVER_URL + ws, TOKEN);
  }

  private void mockWs(String ws, String response) throws Exception {
    when(client.getHttp(eq(SERVER_URL + ws), anyString())).thenReturn(response);
  }

  private String getFile(String name) throws IOException, URISyntaxException {
    URL resource = this.getClass().getResource(name);
    Path p = Paths.get(resource.toURI());
    return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
  }

}
