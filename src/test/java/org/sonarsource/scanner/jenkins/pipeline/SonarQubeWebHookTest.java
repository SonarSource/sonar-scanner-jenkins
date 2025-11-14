/*
 * SonarQube Scanner for Jenkins
 * Copyright (C) 2007-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
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
package org.sonarsource.scanner.jenkins.pipeline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.api.Assertions.assertThrows;

@WithJenkins
class SonarQubeWebHookTest {

  private JenkinsRule jenkins;

  @BeforeEach
  void setUp(JenkinsRule rule) {
    jenkins = rule;
  }

  @WithoutJenkins
  @Test
  void improveCoverage() {
    SonarQubeWebHook aut = new SonarQubeWebHook();
    assertThat(aut.getDisplayName()).isNull();
    assertThat(aut.getIconFileName()).isNull();
    assertThat(aut.getUrlName()).isNotNull();
  }

  @Test
  void invalidPayload() {
    assertThrows(Exception.class, () ->
            jenkins.postJSON("sonarqube-webhook/", "foo"));
  }

  @Test
  void testListener() throws Exception {
    Map<String, String> eventsPerListener = new HashMap<>();

    jenkins.postJSON("sonarqube-webhook/", """
            {
            "taskId":"AVpBJY0hh5C8Sya1ZSgH",
            "status":"SUCCESS",
            "qualityGate":{"status":"OK"},
            "project": {"name": "foo", "url": "http://localhost:9000/dashboard?id=foo"}
            }""");

    SonarQubeWebHook.get().addListener(
            event -> eventsPerListener.put("ListenerA", event.getPayload().getTaskId() + event.getPayload().getTaskStatus() + event.getPayload().getQualityGateStatus()));
    SonarQubeWebHook.get().addListener(
            event -> eventsPerListener.put("ListenerB", event.getPayload().getTaskId() + event.getPayload().getTaskStatus() + event.getPayload().getQualityGateStatus()));

    jenkins.postJSON("sonarqube-webhook/", """
            {
            "taskId":"AVpBJY0hh5C8Sya1ZSgH",
            "status":"SUCCESS",
            "qualityGate":{"status":"OK"},
            "project": {"name": "foo", "url": "http://localhost:9000/dashboard?id=foo"}
            }""");

    assertThat(eventsPerListener).containsOnly(entry("ListenerA", "AVpBJY0hh5C8Sya1ZSgHSUCCESSOK"),
            entry("ListenerB", "AVpBJY0hh5C8Sya1ZSgHSUCCESSOK"));

    eventsPerListener.clear();

    // No quality gate defined
    jenkins.postJSON("sonarqube-webhook/", """
            {
            "taskId":"AVpBJY0hh5C8Sya1ZSgH",
            "status":"SUCCESS",
            "project": {"name": "foo", "url": "http://localhost:9000/dashboard?id=foo"}
            }""");

    assertThat(eventsPerListener).containsOnly(entry("ListenerA", "AVpBJY0hh5C8Sya1ZSgHSUCCESSNONE"),
            entry("ListenerB", "AVpBJY0hh5C8Sya1ZSgHSUCCESSNONE"));
  }

}
