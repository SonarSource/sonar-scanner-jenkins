/*
 * Jenkins :: Integration Tests
 * Copyright (C) 2013-2025 SonarSource SA
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
package com.sonar.it.jenkins.utility;

import com.sonar.orchestrator.Orchestrator;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class InstallableScannerVersionsProvider {
  private static final String TOKEN_PROPERTY = "github.token";
  private static final String TOKEN_ENV_VARIABLE = "GITHUB_TOKEN";

  private final OkHttpClient client = new OkHttpClient().newBuilder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .build();

  public InstallableScannerVersions getScannerInstallableVersions(Orchestrator orchestrator, String scannerNameRepo) {
    JSONArray versions;
    HttpUrl.Builder httpBuilder = HttpUrl.parse(format("https://api.github.com/repos/SonarSource/%s/releases", scannerNameRepo)).newBuilder();
    Request request = new Request.Builder().url(httpBuilder.build())
      .header("Authorization", "token " + loadGithubToken(orchestrator))
      .build();
    try (Response response = client.newCall(request).execute()) {

      if (response.isSuccessful()) {
        versions = new JSONArray(response.body().string());
      } else {
        throw new IllegalStateException(format("Got error from Github, status: %d, body: %s",
          response.code(), response.body().string()));
      }

    } catch (IOException | JSONException e) {
      throw new IllegalStateException(format("Could not fetch earliest supported version of %s", scannerNameRepo), e);
    }
    return new InstallableScannerVersions(
      versions.getJSONObject(versions.length() - 1).getString("tag_name"),
      versions.getJSONObject(0).getString("tag_name")
    );
  }

  private String loadGithubToken(Orchestrator orchestrator) {
    String token = orchestrator.getConfiguration().getString(TOKEN_PROPERTY, orchestrator.getConfiguration().getString(TOKEN_ENV_VARIABLE));
    requireNonNull(token, () -> format("Please provide your GitHub token with the property %s or the env variable %s", TOKEN_PROPERTY, TOKEN_ENV_VARIABLE));
    return token;
  }

  public static class InstallableScannerVersions {
    private final String oldestVersion;
    private final String latestVersion;

    public InstallableScannerVersions(String oldestVersion, String latestVersion) {
      this.oldestVersion = oldestVersion;
      this.latestVersion = latestVersion;
    }

    public String getOldestVersion() {
      return oldestVersion;
    }

    public String getLatestVersion() {
      return latestVersion;
    }
  }
}
