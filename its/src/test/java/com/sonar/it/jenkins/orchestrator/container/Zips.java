/*
 * Copyright (C) 2013-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.jenkins.orchestrator.container;

import com.sonar.orchestrator.config.FileSystem;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

class Zips {
  // Declared as static to share information between orchestrators
  static Set<File> upToDateSnapshots = new HashSet<File>();

  private FileSystem fileSystem;

  Zips(FileSystem fileSystem) {
    this.fileSystem = fileSystem;
  }

  File get(JenkinsDistribution distrib) {
    String filename = "jenkins-war-" + distrib.getVersion() + ".war";

    if (distrib.isRelease()) {
      // releases are kept in user cache
      return new File(fileSystem.sonarInstallsDir(), filename);
    }
    File snapshotWar = new File(fileSystem.workspace(), filename);
    if (snapshotWar.exists() && !isUpToDate(snapshotWar)) {
      LoggerFactory.getLogger(Zips.class).info("Delete deprecated zip: " + snapshotWar);
      FileUtils.deleteQuietly(snapshotWar);
    }
    return snapshotWar;
  }

  private boolean isUpToDate(File snapshotZip) {
    return upToDateSnapshots.contains(snapshotZip);
  }

  void setAsUpToDate(File file) {
    if (file != null && file.exists()) {
      upToDateSnapshots.add(file);
    }
  }
}
