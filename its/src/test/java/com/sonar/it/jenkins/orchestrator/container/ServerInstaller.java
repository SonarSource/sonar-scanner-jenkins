/*
 * Copyright (C) 2013-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.jenkins.orchestrator.container;

import com.sonar.orchestrator.config.FileSystem;
import java.io.File;

/**
 * generate and configure jenkins home
 */
public final class ServerInstaller {
  private final JenkinsDownloader downloader;

  public ServerInstaller(FileSystem fileSystem) {
    this.downloader = new JenkinsDownloader(fileSystem);
  }

  public JenkinsServer install(JenkinsDistribution distribution) {
    File jenkinsBase = downloader.download(distribution);

    return new JenkinsServer(jenkinsBase, distribution);
  }

}
