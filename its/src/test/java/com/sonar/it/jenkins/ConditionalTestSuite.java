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
package com.sonar.it.jenkins;

import com.sonar.it.jenkins.orchestrator.JenkinsOrchestrator;
import com.sonar.orchestrator.version.Version;
import junit.framework.JUnit4TestAdapter;
import junit.framework.TestSuite;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

@RunWith(AllTests.class)
public class ConditionalTestSuite {

  public static TestSuite suite() {
    TestSuite suite = new TestSuite();
    if (isPipelineSupportedJenkinsRuntime()) {
      suite.addTest(new JUnit4TestAdapter(JenkinsPipelineTest.class));
    }
    return suite;
  }

  private static boolean isPipelineSupportedJenkinsRuntime() {
    return Version.create(JenkinsOrchestrator.builderEnv().getJenkinsVersion()).isGreaterThanOrEquals("2");
  }

}
