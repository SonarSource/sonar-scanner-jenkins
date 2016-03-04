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

import org.apache.commons.io.IOUtils;
import org.eclipse.aether.util.StringUtils;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HttpClient {
  public String getHttp(String urlToRead, String username, String password) throws Exception {
    System.out.println(String.format("%s %s %s", urlToRead, username, password));
    URL url = new URL(urlToRead);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

    if (!StringUtils.isEmpty(username)) {
      String userpass;
      if (StringUtils.isEmpty(password)) {
        // to support authentication tokens
        userpass = username;
      } else {
        userpass = username + ":" + password;
      }
      String basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userpass.getBytes(StandardCharsets.UTF_8));
      conn.setRequestProperty("Authorization", basicAuth);
    }

    conn.setRequestMethod("GET");
    return IOUtils.toString(conn.getInputStream(), StandardCharsets.UTF_8.name());
  }
}
