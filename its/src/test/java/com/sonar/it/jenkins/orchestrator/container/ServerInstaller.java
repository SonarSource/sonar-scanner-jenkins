/*
 * Copyright (C) 2013-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.jenkins.orchestrator.container;

import com.sonar.orchestrator.config.FileSystem;
import com.sonar.orchestrator.timings.Timings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * generate and configure jenkins home
 */
public final class ServerInstaller {
  private static final Logger LOG = LoggerFactory.getLogger(ServerInstaller.class);

  private final FileSystem fileSystem;
  private final JenkinsDownloader downloader;

  public ServerInstaller(FileSystem fileSystem) {
    this.fileSystem = fileSystem;
    this.downloader = new JenkinsDownloader(fileSystem);
  }

  public JenkinsServer install(JenkinsDistribution distribution, Timings timings) {
    File jenkinsBase = downloader.download(distribution, timings);

    return new JenkinsServer(jenkinsBase, distribution);
  }

}
