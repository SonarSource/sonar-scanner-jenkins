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
import hudson.plugins.sonar.client.QualityGateResolver;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class QualityGateResolverTest extends SonarTestCase {
  private final static String SERVER_URL = "http://myserver.org";
  private final static String PROJECT_KEY = "org.sonarsource.sonarlint:sonarlint-cli";
  private final static String PROJECT_URL = SERVER_URL + "/dashboard/index/" + PROJECT_KEY;
  private final static String INST_NAME = "my sonarqube";
  private final static String PASS = "mypass";
  private final static String USER = "user";

  private QualityGateResolver resolver;
  private HttpClient client;

  @Before
  public void setUp() {
    super.configureSonar(new SonarInstallation(INST_NAME, SERVER_URL, null, null, null, null, null, null, USER, PASS, null));
    client = mock(HttpClient.class);
    resolver = new QualityGateResolver(client);
  }

  @Test
  public void testQG55() throws Exception {
    mockSQServer(5.5f);
    ProjectInformation proj = resolver.get(PROJECT_URL, INST_NAME);
    assertThat(proj).isNotNull();
    assertThat(proj.getStatus()).isEqualTo("OK");
    assertThat(proj.getProjectName()).isEqualTo("SonarLint CLI");
    assertThat(proj.getProjectKey()).isEqualTo("org.sonarsource.sonarlint:sonarlint-cli");
    assertThat(proj.getErrors()).isNullOrEmpty();

    verify(client).getHttp(Mockito.startsWith(SERVER_URL + WsClient.API_PROJECT_NAME), eq(USER), eq(PASS));
  }

  @Test
  public void testQG51() throws Exception {
    mockSQServer(5.1f);
    ProjectInformation proj = resolver.get(PROJECT_URL, INST_NAME);
    assertThat(proj).isNotNull();
    assertThat(proj.getStatus()).isEqualTo("WARN");
    assertThat(proj.getProjectName()).isEqualTo("SonarLint CLI");
    assertThat(proj.getProjectKey()).isEqualTo("org.sonarsource.sonarlint:sonarlint-cli");
    assertThat(proj.getErrors()).isNullOrEmpty();

    verify(client).getHttp(Mockito.startsWith(SERVER_URL + WsClient.API_RESOURCES), eq(USER), eq(PASS));
  }

  @Test
  public void testWsError() throws Exception {
    mockSQServer(new NullPointerException());
    ProjectInformation proj = resolver.get(PROJECT_URL, INST_NAME);
    assertThat(proj).isNull();
  }

  @Test
  public void testInvalidInstallation() {
    // this will erase previously configured installation INST_NAME
    super.configureDefaultSonar();
    ProjectInformation proj = resolver.get(PROJECT_URL, INST_NAME);
    assertThat(proj).isNull();
  }

  private void mockSQServer(Exception toThrow) throws Exception {
    when(client.getHttp(SERVER_URL + WsClient.API_VERSION, null, null)).thenThrow(toThrow);
  }

  private void mockSQServer(float version) throws Exception {
    when(client.getHttp(SERVER_URL + WsClient.API_VERSION, null, null)).thenReturn(Float.toString(version));
    if (version >= 5.2) {
      when(client.getHttp(Mockito.startsWith(SERVER_URL + WsClient.API_PROJECT_STATUS), eq(USER), eq(PASS))).thenReturn(getFile("projectStatus.json"));
      when(client.getHttp(Mockito.startsWith(SERVER_URL + WsClient.API_PROJECT_NAME), eq(USER), eq(PASS))).thenReturn(getFile("projectIndex.json"));
    } else {
      when(client.getHttp(Mockito.startsWith(SERVER_URL + WsClient.API_RESOURCES), eq(USER), eq(PASS))).thenReturn(getFile("resources.json"));
    }
  }

  private String getFile(String name) throws IOException, URISyntaxException {
    URL resource = this.getClass().getResource(name);
    Path p = Paths.get(resource.toURI());
    return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
  }

}
