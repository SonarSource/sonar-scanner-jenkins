/*
 * SonarQube Scanner for Jenkins
 * Copyright (C) 2007-2024 SonarSource SA
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import okhttp3.Call;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonarqube.ws.client.HttpException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HttpClientTest {
  private static final String URL = "http://sonarqube.org";
  Call call = mock(Call.class);
  ResponseBody body = mock(ResponseBody.class);
  OkHttpClient okHttpClient = mock(OkHttpClient.class);
  BufferedSource source = mock(BufferedSource.class);
  HttpClient underTest = new HttpClient(okHttpClient);
  Request request = new Request.Builder().url(URL).build();
  Response response = new Response.Builder().code(200).body(body).protocol(Protocol.HTTP_2).message("message").request(request).build();

  @Before
  public void setUp() throws IOException {
    when(source.readString(any())).thenReturn("body");
    when(okHttpClient.newCall(any())).thenReturn(call);
    when(call.execute()).thenReturn(response);
    when(body.source()).thenReturn(source);
    when(body.string()).thenCallRealMethod();
  }

  @Test
  public void request_successful_should_return_content() throws IOException {
    ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);

    String content = underTest.getHttp(URL, "token");
    verify(okHttpClient).newCall(requestCaptor.capture());

    assertThat(requestCaptor.getValue().header("Authorization")).isEqualTo(Credentials.basic("token", "", StandardCharsets.UTF_8));
    assertThat(content).isEqualTo("body");
  }

  @Test
  public void request_fail_should_throw_http_exception() throws IOException {
    Response failedResponse = new Response.Builder().code(401).body(body).protocol(Protocol.HTTP_2).message("message").request(request).build();
    when(call.execute()).thenReturn(failedResponse);

    assertThatThrownBy(() -> underTest.getHttp(URL, null))
      .isInstanceOf(HttpException.class)
      .hasMessage("Error 401 on http://sonarqube.org : body");
  }

  @Test
  public void network_error_should_throw_illegal_state_exception() throws IOException {
    when(call.execute()).thenThrow(new IOException());

    assertThatThrownBy(() -> underTest.getHttp(URL, null))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Fail to request http://sonarqube.org/");
  }

  @Test
  public void fail_to_read_response_should_throw_illegal_state_exception() throws IOException {
    when(source.readString(any())).thenThrow(new IOException());

    assertThatThrownBy(() -> underTest.getHttp(URL, null))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Fail to read response of http://sonarqube.org/");
  }

}
