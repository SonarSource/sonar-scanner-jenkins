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

import hudson.plugins.sonar.client.WsClient.ProjectQualityGate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WsClientTest {
  private final static String SERVER_URL = "http://myserver.org";
  private final static String PROJECT_KEY = "org.sonarsource.sonarlint:sonarlint-cli"; 
  private final static String PASS = "mypass";
  private final static String USER = "user";
  
  private HttpClient client = mock(HttpClient.class);
  private WsClient wsClient;
  
  @Rule
  public ExpectedException exception = ExpectedException.none();
  
  @Before
  public void setUp() {
    wsClient = new WsClient(client, SERVER_URL, PROJECT_KEY, USER, PASS);
  }
  
  @Test
  public void testQG52() throws Exception {
    String ws = "/api/qualitygates/project_status?projectKey=" + PROJECT_KEY;
    setSQVersion(5.2f);
    String json = getFile("projectStatus.json");
    String projectIndex = getFile("projectIndex.json");
    mockWs(ws, json);
    mockWs("/api/projects/index?format=json&key=" + PROJECT_KEY, projectIndex);

    ProjectQualityGate qg = wsClient.getQualityGate52();
    
    assertThat(qg.getProjectName()).isEqualTo("SonarLint CLI");
    assertThat(qg.getStatus()).isEqualTo("OK");
    verifyWs(ws);
  }

  @Test
  public void testQGBefore52() throws Exception {
    String ws = "/api/resources?format=json&depth=0&metrics=alert_status&resource=" + PROJECT_KEY;
    setSQVersion(5.1f);
    String json = getFile("resources.json");
    mockWs(ws, json);
    
    ProjectQualityGate qg = wsClient.getQualityGateBefore52();
    
    assertThat(qg.getProjectName()).isEqualTo("SonarLint CLI");
    assertThat(qg.getStatus()).isEqualTo("WARN");
    
    verifyWs(ws);
  }
  
  @Test
  public void testGetVersion() throws Exception {
    setSQVersion(5.1f);
    
    assertThat(wsClient.getServerVersion(SERVER_URL)).isEqualTo("5.1");
    verify(client).getHttp(SERVER_URL + "/api/server/version", null, null);
  }

  @Test
  public void testConnectionError() throws Exception {
    when(client.getHttp(anyString(), anyString(), anyString())).thenThrow(Exception.class);

    exception.expect(Exception.class);
    wsClient.getQualityGate52();
  }

  private void setSQVersion(float version) throws Exception {
    when(client.getHttp(SERVER_URL + "/api/server/version", null, null)).thenReturn(Float.toString(version));
  }

  private void verifyWs(String ws) throws Exception {
    verify(client).getHttp(SERVER_URL + ws, USER, PASS);
  }

  private void mockWs(String ws, String response) throws Exception {
    when(client.getHttp(eq(SERVER_URL + ws), anyString(), anyString())).thenReturn(response);
  }
  
  private String getFile(String name) throws IOException, URISyntaxException {
    URL resource = this.getClass().getResource(name);
    Path p = Paths.get(resource.toURI());
    return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
  }
  
}
