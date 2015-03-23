/*
 * Copyright (C) 2013-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.jenkins;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.locator.FileLocation;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({JenkinsTest.class})
public class JenkinsTestSuite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    // TODO Java projects should be replaced by Xoo projects
    .setOrchestratorProperty("javaVersion", "LATEST_RELEASE")
    .addPlugin("java")
    .restoreProfileAtStartup(FileLocation.ofClasspath("/com/sonar/it/jenkins/JenkinsTest/sonar-way-it-profile_java.xml"))
    .build();

}
