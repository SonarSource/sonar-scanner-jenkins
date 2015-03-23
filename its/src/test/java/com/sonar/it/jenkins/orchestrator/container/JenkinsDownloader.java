/*
 * Copyright (C) 2013-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.jenkins.orchestrator.container;

import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.sonar.orchestrator.config.FileSystem;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.orchestrator.timings.Step;
import com.sonar.orchestrator.timings.Timings;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Random;

public class JenkinsDownloader {
  private static final Logger LOG = LoggerFactory.getLogger(JenkinsDownloader.class);

  private final FileSystem fileSystem;
  private final Zips zips;

  public JenkinsDownloader(FileSystem fileSystem) {
    this.fileSystem = fileSystem;
    this.zips = new Zips(fileSystem);
  }

  public synchronized File download(JenkinsDistribution distrib, Timings timings) {
    LOG.info("Downloading Jenkins-" + distrib.getVersion());

    String key = generateKey();

    File toDir = new File(fileSystem.workspace(), key);
    if (toDir.exists()) {
      try {
        FileUtils.cleanDirectory(toDir);
      } catch (IOException e) {
        throw new IllegalStateException("Fail to clean directory: " + toDir, e);
      }
    }

    LOG.info("Download Jenkins-" + distrib.getVersion() + " in " + toDir.getAbsolutePath());
    timings.begin(Step.DOWNLOAD);
    File war = downloadWar(distrib);
    timings.finish(Step.DOWNLOAD);

    LOG.info("Copy " + war);
    timings.begin(Step.UNZIP);
    try {
      FileUtils.copyFileToDirectory(war, toDir);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    timings.finish(Step.UNZIP);

    return toDir;
  }

  static String generateKey() {
    return String.valueOf(new Random().nextInt(1000));
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
      String url = "http://mirrors.jenkins-ci.org/war/" + distribution.getVersion() + "/jenkins.war";
      return downloadUrl(url, toFile);
    }
    return null;
  }

  private File downloadUrl(String url, File toFile) {
    try {
      FileUtils.forceMkdir(toFile.getParentFile());

      URL u = new URL(url);

      LOG.info("Download: " + u);
      Files.copy(Resources.newInputStreamSupplier(u), toFile);
      LOG.info("Downloaded to: " + toFile);

      return toFile;
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
