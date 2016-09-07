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
