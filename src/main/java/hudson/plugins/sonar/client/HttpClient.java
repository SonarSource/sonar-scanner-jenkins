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

import org.apache.commons.lang.StringUtils;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsResponse;

public class HttpClient {
  public String getHttp(String url, String usernameOrToken, String password) {
    String baseUrl = StringUtils.substringBeforeLast(url, "/");
    String path = StringUtils.substringAfterLast(url, "/");
    HttpConnector httpConnector = HttpConnector.newBuilder()
      .userAgent("Scanner for Jenkins")
      .url(baseUrl)
      .credentials(usernameOrToken, password)
      .build();
    WsResponse response = httpConnector.call(new GetRequest(path));
    response.failIfNotSuccessful();
    return response.content();
  }
}
