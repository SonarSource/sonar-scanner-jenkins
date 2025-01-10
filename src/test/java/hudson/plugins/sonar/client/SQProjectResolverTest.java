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
package hudson.plugins.sonar.client;

import hudson.model.Run;
import hudson.plugins.sonar.SonarInstallation;
import hudson.plugins.sonar.SonarTestCase;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.sonarqube.ws.client.HttpException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class SQProjectResolverTest extends SonarTestCase {
  private final static String SERVER_URL = "http://localhost:9000";
  private final static String PROJECT_KEY = "org.sonarsource.sonarlint:sonarlint-cli";
  private final static String PROJECT_URL = SERVER_URL + "/dashboard/index/" + PROJECT_KEY;
  private final static String CE_TASK_ID = "task1";
  private final static String TOKEN = "token";
  private final static String CREDENTIAL_ID = "cred-id";

  @Rule
  public TestName testName = new TestName();

  private SQProjectResolver resolver;
  private HttpClient client;

  @Before
  public void setUp() {
    configureDefaultSonar();
    client = mock(HttpClient.class);
    resolver = new SQProjectResolver(client);
  }

  @Test
  public void testSQ() throws Exception {
    mockSQServer();
    ProjectInformation proj = resolver.resolve(SERVER_URL, PROJECT_URL, CE_TASK_ID, testName.getMethodName(), mock(Run.class));
    assertThat(proj).isNotNull();
    assertThat(proj.getCeStatus()).isEqualTo("success");
    assertThat(proj.getStatus()).isEqualTo("OK");
    assertThat(proj.getProjectName()).isEqualTo("SonarLint CLI");
    assertThat(proj.getErrors()).isNullOrEmpty();

    verify(client).getHttp(Mockito.startsWith(SERVER_URL + WsClient.API_PROJECT_STATUS_WITH_ANALYSISID), eq(TOKEN));
    verify(client).getHttp(Mockito.startsWith(SERVER_URL + WsClient.API_CE_TASK), eq(TOKEN));

    verifyNoMoreInteractions(client);
  }

  @Test
  public void testInvalidServerUrl() {
    ProjectInformation proj = resolver.resolve("invalid", PROJECT_URL, CE_TASK_ID, testName.getMethodName(), mock(Run.class));
    assertThat(proj).isNull();
  }

  @Test
  public void testWsError() {
    mockSQServer(new NullPointerException());
    ProjectInformation proj = resolver.resolve(SERVER_URL, PROJECT_URL, null, testName.getMethodName(), mock(Run.class));
    assertThat(proj).isNull();
  }

  @Test
  public void testWsHttpNotFound() {
    SonarInstallation inst = spy(new SonarInstallation(testName.getMethodName(), SERVER_URL, CREDENTIAL_ID, null, null, null, null,
      null, null));
    addCredential(CREDENTIAL_ID, TOKEN);
    configureSonar(inst);

    when(client.getHttp(startsWith(SERVER_URL + WsClient.API_CE_TASK), eq(TOKEN))).thenThrow(new HttpException(SERVER_URL, 404, "oops"));
    ProjectInformation proj = resolver.resolve(SERVER_URL, PROJECT_URL, null, testName.getMethodName(), mock(Run.class));
    assertThat(proj).isNull();
  }

  @Test
  public void testWsHttpError() {
    SonarInstallation inst = spy(new SonarInstallation(testName.getMethodName(), SERVER_URL, CREDENTIAL_ID, null, null, null, null,
      null, null));
    addCredential(CREDENTIAL_ID, TOKEN);
    configureSonar(inst);

    when(client.getHttp(startsWith(SERVER_URL + WsClient.API_CE_TASK), eq(TOKEN))).thenThrow(new HttpException(SERVER_URL, 500, "oops"));
    ProjectInformation proj = resolver.resolve(SERVER_URL, PROJECT_URL, null, testName.getMethodName(), mock(Run.class));
    assertThat(proj).isNull();
  }

  @Test
  public void testInvalidInstallation() {
    configureDefaultSonar();
    ProjectInformation proj = resolver.resolve(SERVER_URL, PROJECT_URL, null, "INVALID", mock(Run.class));
    assertThat(proj).isNull();
  }

  @Override
  protected SonarInstallation configureDefaultSonar() {
    return configureSonar(new SonarInstallation(testName.getMethodName(), null, null, null, null, null, null, null, null));
  }

  private void mockSQServer(Exception toThrow) {
    when(client.getHttp(SERVER_URL + WsClient.API_VERSION, null)).thenThrow(toThrow);
  }

  private void mockSQServer() throws Exception {
    SonarInstallation inst = spy(new SonarInstallation(testName.getMethodName(), SERVER_URL, CREDENTIAL_ID, null, null, null, null,
        null, null));
    addCredential(CREDENTIAL_ID, TOKEN);
    configureSonar(inst);

    when(client.getHttp(startsWith(SERVER_URL + WsClient.API_PROJECT_STATUS_WITH_ANALYSISID), eq(TOKEN))).thenReturn(getFile("projectStatus.json"));
    when(client.getHttp(startsWith(SERVER_URL + WsClient.API_CE_TASK), eq(TOKEN))).thenReturn(getFile("ce_task.json"));
  }

  private String getFile(String name) throws IOException, URISyntaxException {
    URL resource = getClass().getResource(name);
    Path p = Paths.get(resource.toURI());
    return Files.readString(p);
  }

}
