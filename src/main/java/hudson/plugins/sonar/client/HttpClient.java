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

import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsResponse;

public class HttpClient {
  public String getHttp(String url, @Nullable String token, @Nullable Float timeout_ms) {
    String baseUrl = StringUtils.substringBeforeLast(url, "/");
    String path = StringUtils.substringAfterLast(url, "/");
    Float connect_timout = defaultIfBlank(timeout_ms, 5_000)
    Float read_timout = defaultIfBlank(timeout_ms, 5_000)
    HttpConnector httpConnector = HttpConnector.newBuilder()
      .readTimeoutMilliseconds(read_timout)
      .connectTimeoutMilliseconds(connect_timout)
      .userAgent("Scanner for Jenkins")
      .url(baseUrl)
      .credentials(token, null)
      .build();
    WsResponse response = httpConnector.call(new GetRequest(path));
    response.failIfNotSuccessful();
    return response.content();
  }
}
