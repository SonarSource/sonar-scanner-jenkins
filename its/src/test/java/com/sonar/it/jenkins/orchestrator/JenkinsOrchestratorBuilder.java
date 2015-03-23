/*
 * Copyright (C) 2013-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.jenkins.orchestrator;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.sonar.it.jenkins.orchestrator.container.JenkinsDistribution;
import com.sonar.orchestrator.config.Configuration;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;

import java.util.Map;

public class JenkinsOrchestratorBuilder {

  public static final String JENKINS_VERSION_PROPERTY = "jenkins.runtimeVersion";

  private final Configuration config;
  private final JenkinsDistribution distribution;
  private final Map<String, String> overriddenProperties;

  JenkinsOrchestratorBuilder(Configuration initialConfig) {
    this.config = initialConfig;
    this.distribution = new JenkinsDistribution();

    this.overriddenProperties = Maps.newHashMap();
  }

  public JenkinsOrchestratorBuilder setJenkinsVersion(String s) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(s), "Empty jenkins version");
    this.overriddenProperties.put(JENKINS_VERSION_PROPERTY, s);
    return this;
  }

  public String getJenkinsVersion() {
    return getOrchestratorProperty(JENKINS_VERSION_PROPERTY);
  }

  public JenkinsOrchestratorBuilder setOrchestratorProperty(String key, @Nullable String value) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(key), "Empty property key");
    overriddenProperties.put(key, value);
    return this;
  }

  public String getOrchestratorProperty(String key) {
    return StringUtils.defaultString(overriddenProperties.get(key), config.getString(key));
  }

  public Configuration getOrchestratorConfiguration() {
    return this.config;
  }

  public JenkinsOrchestrator build() {
    Configuration.Builder configBuilder = Configuration.builder();
    Configuration finalConfig = configBuilder.addConfiguration(config).addMap(overriddenProperties).build();

    String version = getJenkinsVersion();
    Preconditions.checkState(!Strings.isNullOrEmpty(version), "Missing Jenkins version");

    this.distribution.setVersion(version);
    return new JenkinsOrchestrator(finalConfig, distribution);
  }
}
