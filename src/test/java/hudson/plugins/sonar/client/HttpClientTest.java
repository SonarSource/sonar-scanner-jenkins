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

import okhttp3.Call;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sonarqube.ws.client.HttpException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HttpClientTest {
  private static final String URL = "http://sonarqube.org";
  private final Call call = mock(Call.class);
  private final ResponseBody body = mock(ResponseBody.class);
  private final OkHttpClient okHttpClient = mock(OkHttpClient.class);
  private final BufferedSource source = mock(BufferedSource.class);
  private final HttpClient underTest = new HttpClient(okHttpClient);
  private final Request request = new Request.Builder().url(URL).build();
  private final Response response = new Response.Builder().code(200).body(body).protocol(Protocol.HTTP_2).message("message").request(request).build();

  @BeforeEach
  void setUp() throws IOException {
    when(source.readString(any())).thenReturn("body");
    when(okHttpClient.newCall(any())).thenReturn(call);
    when(call.execute()).thenReturn(response);
    when(body.source()).thenReturn(source);
    when(body.string()).thenCallRealMethod();
  }

  @Test
  void request_successful_should_return_content() {
    ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);

    String content = underTest.getHttp(URL, "token");
    verify(okHttpClient).newCall(requestCaptor.capture());

    assertThat(requestCaptor.getValue().header("Authorization")).isEqualTo(Credentials.basic("token", "", StandardCharsets.UTF_8));
    assertThat(content).isEqualTo("body");
  }

  @Test
  void request_fail_should_throw_http_exception() throws Exception {
    Response failedResponse = new Response.Builder().code(401).body(body).protocol(Protocol.HTTP_2).message("message").request(request).build();
    when(call.execute()).thenReturn(failedResponse);

    assertThatThrownBy(() -> underTest.getHttp(URL, null))
            .isInstanceOf(HttpException.class)
            .hasMessage("Error 401 on http://sonarqube.org : body");
  }

  @Test
  void network_error_should_throw_illegal_state_exception() throws Exception {
    when(call.execute()).thenThrow(new IOException());

    assertThatThrownBy(() -> underTest.getHttp(URL, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Fail to request http://sonarqube.org/");
  }

  @Test
  void fail_to_read_response_should_throw_illegal_state_exception() throws Exception {
    when(source.readString(any())).thenThrow(new IOException());

    assertThatThrownBy(() -> underTest.getHttp(URL, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Fail to read response of http://sonarqube.org/");
  }

}
