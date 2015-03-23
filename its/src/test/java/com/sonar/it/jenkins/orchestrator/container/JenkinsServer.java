/*
 * Copyright (C) 2013-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.jenkins.orchestrator.container;

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

  public String getVersion() {
    return distribution.getVersion();
  }

}
