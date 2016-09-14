/*
 * Jenkins :: Integration Tests
 * Copyright (C) 2013-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package com.sonar.it.jenkins.orchestrator.container;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.sonar.orchestrator.config.FileSystem;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.locator.MavenLocation;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JenkinsDownloader {
  private static final Logger LOG = LoggerFactory.getLogger(JenkinsDownloader.class);

  private static final AtomicInteger sharedDirId = new AtomicInteger(0);
  private final FileSystem fileSystem;
  private final Zips zips;

  public JenkinsDownloader(FileSystem fileSystem) {
    this.fileSystem = fileSystem;
    this.zips = new Zips(fileSystem);
  }

  public synchronized File download(JenkinsDistribution distrib) {
    LOG.info("Downloading Jenkins-" + distrib.getVersion());

    // Add a "j" prefix to not conflict with SonarQube
    File toDir = new File(fileSystem.workspace(), "j" + String.valueOf(sharedDirId.addAndGet(1)));
    if (toDir.exists()) {
      try {
        FileUtils.cleanDirectory(toDir);
      } catch (IOException e) {
        throw new IllegalStateException("Fail to clean directory: " + toDir, e);
      }
    }

    LOG.info("Download Jenkins-" + distrib.getVersion() + " in " + toDir.getAbsolutePath());
    File war = downloadWar(distrib);

    LOG.info("Copy " + war);
    try {
      FileUtils.copyFileToDirectory(war, toDir);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return toDir;
  }

  public File downloadWar(JenkinsDistribution distrib) {
    File war = zips.get(distrib);
    if (war.exists()) {
      LOG.info("Jenkins found: " + war);
    } else {
      war = downloadWarToFile(distrib, war);
      zips.setAsUpToDate(war);
    }
    return war;
  }

  private File downloadWarToFile(JenkinsDistribution distrib, File toFile) {
    File zip = searchInMavenRepositories(distrib, toFile);
    if (zip == null || !zip.exists()) {
      zip = downloadFromJenkinsCi(distrib, toFile);
    }
    if (zip == null || !zip.exists() || !zip.isFile()) {
      throw new IllegalStateException("Can not find Jenkins " + distrib.getVersion());
    }
    return zip;
  }

  private File searchInMavenRepositories(JenkinsDistribution distribution, File toFile) {
    LOG.info("Searching for Jenkins in Maven repositories");
    // search for zip in maven repositories
    Location location = getMavenLocation(distribution);
    File result = fileSystem.copyToFile(location, toFile);
    if (result != null && result.exists()) {
      LOG.info("Found Jenkins in Maven repositories");
    }
    return result;
  }

  private Location getMavenLocation(JenkinsDistribution distribution) {
    return MavenLocation.builder()
      .setGroupId("org.jenkins-ci.main")
      .setArtifactId("jenkins-war")
      .setVersion(distribution.getVersion())
      .withPackaging("war")
      .build();
  }

  private File downloadFromJenkinsCi(JenkinsDistribution distribution, File toFile) {
    if (distribution.isRelease()) {
      String url = "https://updates.jenkins-ci.org/download/war/" + distribution.getVersion() + "/jenkins.war";
      return downloadUrl(url, toFile);
    }
    return null;
  }

  private File downloadUrl(String url, File toFile) {
    try {
      FileUtils.forceMkdir(toFile.getParentFile());

      HttpURLConnection conn;
      URL u = new URL(url);

      LOG.info("Download: " + u);
      // gets redirected multiple times, including from https -> http, so not done by Java

      while (true) {
        conn = (HttpURLConnection) u.openConnection();
        conn.connect();
        if (conn.getResponseCode() == HttpURLConnection.HTTP_MOVED_PERM
          || conn.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
          String newLocationHeader = conn.getHeaderField("Location");
          LOG.info("Redirect: " + newLocationHeader);
          conn.disconnect();
          u = new URL(newLocationHeader);
          continue;
        }
        break;
      }

      InputStream is = conn.getInputStream();
      ByteStreams.copy(is, Files.asByteSink(toFile).openBufferedStream());
      LOG.info("Downloaded to: " + toFile);
      return toFile;
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }

  }
}
