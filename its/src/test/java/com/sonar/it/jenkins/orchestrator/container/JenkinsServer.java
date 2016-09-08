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

import com.sonar.orchestrator.version.Version;
import java.io.File;

public class JenkinsServer {

  private final File base;
  private final JenkinsDistribution distribution;
  private String url;

  public JenkinsServer(File base, JenkinsDistribution distribution) {
    this.base = base;
    this.distribution = distribution;
  }

  public File getHome() {
    return new File(base, "work");
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public JenkinsDistribution getDistribution() {
    return distribution;
  }

  public Version getVersion() {
    return Version.create(distribution.getVersion());
  }

}
