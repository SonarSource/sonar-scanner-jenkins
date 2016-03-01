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
import hudson.plugins.sonar.SonarTestCase;
import hudson.plugins.sonar.client.SQProjectResolver;
import hudson.plugins.sonar.utils.SQServerVersions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class SQProjectResolverTest extends SonarTestCase {
  private final static String SERVER_URL = "http://localhost:9000";
  private final static String PROJECT_KEY = "org.sonarsource.sonarlint:sonarlint-cli";
  private final static String PROJECT_URL = SERVER_URL + "/dashboard/index/" + PROJECT_KEY;
  private final static String CE_TASK_ID = "task1";
  private final static String PASS = "mypass";
  private final static String USER = "user";
  private final static String TOKEN = "token";

  private SQProjectResolver resolver;
  private HttpClient client;

  @Before
  public void setUp() {
    super.configureDefaultSonar();
    client = mock(HttpClient.class);
    resolver = new SQProjectResolver(client);
  }

  @Test
  public void testSQ54() throws Exception {
    mockSQServer54();
    ProjectInformation proj = resolver.resolve(PROJECT_URL, CE_TASK_ID, SONAR_INSTALLATION_NAME);
    assertThat(proj).isNotNull();
    assertThat(proj.getCeStatus()).isEqualTo("success");
    assertThat(proj.getStatus()).isEqualTo("OK");
    assertThat(proj.getProjectName()).isEqualTo("SonarLint CLI");
    assertThat(proj.getProjectKey()).isEqualTo("org.sonarsource.sonarlint:sonarlint-cli");
    assertThat(proj.getErrors()).isNullOrEmpty();

    verify(client).getHttp(Mockito.startsWith(SERVER_URL + WsClient.API_PROJECT_STATUS), eq(TOKEN), isNull(String.class));
    verify(client).getHttp(Mockito.startsWith(SERVER_URL + WsClient.API_CE_TASK), eq(TOKEN), isNull(String.class));
    verify(client).getHttp(Mockito.startsWith(SERVER_URL + WsClient.API_VERSION), isNull(String.class), isNull(String.class));

    verifyNoMoreInteractions(client);
  }

  @Test
  public void testInvalidServerVersion() throws Exception {
    super.configureDefaultSonar(SQServerVersions.SQ_5_3_OR_HIGHER);
    when(client.getHttp(SERVER_URL + WsClient.API_VERSION, null, null)).thenReturn("55");
    ProjectInformation proj = resolver.resolve(PROJECT_URL, CE_TASK_ID, SONAR_INSTALLATION_NAME);
    assertThat(proj).isNull();
  }

  @Test
  public void testInvalidProjectUrl() {
    ProjectInformation proj = resolver.resolve("invalid", CE_TASK_ID, SONAR_INSTALLATION_NAME);
    assertThat(proj).isNull();
  }

  @Test
  public void testSQ54NoCETask() throws Exception {
    mockSQServer54();
    when(client.getHttp(startsWith(SERVER_URL + WsClient.API_PROJECT_NAME), eq(TOKEN), isNull(String.class))).thenReturn(getFile("projectIndex.json"));

    ProjectInformation proj = resolver.resolve(PROJECT_URL, null, SONAR_INSTALLATION_NAME);
    assertThat(proj).isNotNull();
    assertThat(proj.getCeStatus()).isNull();
    assertThat(proj.getStatus()).isEqualTo("OK");
    assertThat(proj.getProjectName()).isEqualTo("SonarLint CLI");
    assertThat(proj.getProjectKey()).isEqualTo("org.sonarsource.sonarlint:sonarlint-cli");
    assertThat(proj.getErrors()).isNullOrEmpty();

    verify(client).getHttp(Mockito.startsWith(SERVER_URL + WsClient.API_PROJECT_STATUS), eq(TOKEN), isNull(String.class));
    verify(client).getHttp(Mockito.startsWith(SERVER_URL + WsClient.API_PROJECT_NAME), eq(TOKEN), isNull(String.class));
    verify(client).getHttp(Mockito.startsWith(SERVER_URL + WsClient.API_VERSION), isNull(String.class), isNull(String.class));
    verifyNoMoreInteractions(client);
  }

  @Test
  public void testSQ51_no_QG() throws Exception {
    mockSQServer51();
    when(client.getHttp(startsWith(SERVER_URL + WsClient.API_RESOURCES), eq(USER), eq(PASS))).thenReturn("[]");
    ProjectInformation proj = resolver.resolve(PROJECT_URL, null, SONAR_INSTALLATION_NAME);
    assertThat(proj).isNotNull();
    assertThat(proj.getCeStatus()).isNull();
    assertThat(proj.getStatus()).isNull();
    assertThat(proj.getProjectName()).isNull();
    assertThat(proj.getProjectKey()).isEqualTo("org.sonarsource.sonarlint:sonarlint-cli");
    assertThat(proj.getErrors()).isNullOrEmpty();

    verify(client).getHttp(Mockito.startsWith(SERVER_URL + WsClient.API_RESOURCES), eq(USER), eq(PASS));
    verify(client).getHttp(Mockito.startsWith(SERVER_URL + WsClient.API_VERSION), isNull(String.class), isNull(String.class));
    verifyNoMoreInteractions(client);
  }

  @Test
  public void testSQ51() throws Exception {
    super.configureDefaultSonar(SQServerVersions.SQ_5_1_OR_LOWER);
    mockSQServer51();
    ProjectInformation proj = resolver.resolve(PROJECT_URL, null, SONAR_INSTALLATION_NAME);
    assertThat(proj).isNotNull();
    assertThat(proj.getCeStatus()).isNull();
    assertThat(proj.getStatus()).isEqualTo("WARN");
    assertThat(proj.getProjectName()).isEqualTo("SonarLint CLI");
    assertThat(proj.getProjectKey()).isEqualTo("org.sonarsource.sonarlint:sonarlint-cli");
    assertThat(proj.getErrors()).isNullOrEmpty();

    verify(client).getHttp(Mockito.startsWith(SERVER_URL + WsClient.API_RESOURCES), eq(USER), eq(PASS));
    verify(client).getHttp(Mockito.startsWith(SERVER_URL + WsClient.API_VERSION), isNull(String.class), isNull(String.class));
    verifyNoMoreInteractions(client);
  }

  @Test
  public void testWsError() throws Exception {
    mockSQServer(new NullPointerException());
    ProjectInformation proj = resolver.resolve(PROJECT_URL, null, SONAR_INSTALLATION_NAME);
    assertThat(proj).isNull();
  }

  @Test
  public void testInvalidInstallation() {
    super.configureDefaultSonar();
    ProjectInformation proj = resolver.resolve(PROJECT_URL, null, "INVALID");
    assertThat(proj).isNull();
  }

  private void mockSQServer(Exception toThrow) throws Exception {
    when(client.getHttp(SERVER_URL + WsClient.API_VERSION, null, null)).thenThrow(toThrow);
  }

  private void mockSQServer54() throws Exception {
    super.configureSonar(new SonarInstallation(SONAR_INSTALLATION_NAME, SERVER_URL, SQServerVersions.SQ_5_3_OR_HIGHER, TOKEN,
      null, null, null, null, null, null, null, null, null));

    when(client.getHttp(SERVER_URL + WsClient.API_VERSION, null, null)).thenReturn("5.4");
    when(client.getHttp(startsWith(SERVER_URL + WsClient.API_PROJECT_STATUS), eq(TOKEN), isNull(String.class))).thenReturn(getFile("projectStatus.json"));
    when(client.getHttp(startsWith(SERVER_URL + WsClient.API_CE_TASK), eq(TOKEN), isNull(String.class))).thenReturn(getFile("ce_task.json"));
  }

  private void mockSQServer51() throws Exception {
    super.configureSonar(new SonarInstallation(SONAR_INSTALLATION_NAME, SERVER_URL, SQServerVersions.SQ_5_1_OR_LOWER,
      null, null, null, null, null, null, null, USER, PASS, null));

    when(client.getHttp(SERVER_URL + WsClient.API_VERSION, null, null)).thenReturn("5.1");
    when(client.getHttp(startsWith(SERVER_URL + WsClient.API_RESOURCES), eq(USER), eq(PASS))).thenReturn(getFile("resources.json"));
  }

  private String getFile(String name) throws IOException, URISyntaxException {
    URL resource = this.getClass().getResource(name);
    Path p = Paths.get(resource.toURI());
    return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
  }

}
