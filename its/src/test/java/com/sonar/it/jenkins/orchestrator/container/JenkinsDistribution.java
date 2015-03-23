/*
 * Copyright (C) 2013-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.jenkins.orchestrator.container;

import org.apache.commons.lang.StringUtils;

public final class JenkinsDistribution {

  private String version;
  private int port;

  public JenkinsDistribution() {
  }

  public JenkinsDistribution(String version) {
    this.version = version;
  }

  public JenkinsDistribution setVersion(String s) {
    this.version = s;
    return this;
  }

  public String getVersion() {
    return version;
  }

  public boolean isRelease() {
    return !StringUtils.endsWith(version, "-SNAPSHOT");
  }

  public int getPort() {
    return port;
  }

  public JenkinsDistribution setPort(int port) {
    this.port = port;
    return this;
  }
}
