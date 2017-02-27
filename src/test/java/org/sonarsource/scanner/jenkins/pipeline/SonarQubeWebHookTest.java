/*
 * Jenkins Plugin for SonarQube, open source software quality management tool.
 * mailto:contact AT sonarsource DOT com
 *
 * Jenkins Plugin for SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Jenkins Plugin for SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.scanner.jenkins.pipeline;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class SonarQubeWebHookTest {

  @Rule
  public JenkinsRule jenkins = new JenkinsRule();

  @WithoutJenkins
  @Test
  public void improveCoverage() {
    SonarQubeWebHook aut = new SonarQubeWebHook();
    assertThat(aut.getDisplayName()).isNull();
    assertThat(aut.getIconFileName()).isNull();
    assertThat(aut.getUrlName()).isNotNull();
  }

  @Test(expected = Exception.class)
  public void invalidPayload() throws Exception {
    jenkins.postJSON("sonarqube-webhook/", "foo");
  }

  @Test
  public void testListener() throws Exception {

    Map<String, String> eventsPerListener = new HashMap<>();

    jenkins.postJSON("sonarqube-webhook/", "{\n" +
      "\"taskId\":\"AVpBJY0hh5C8Sya1ZSgH\",\n" +
      "\"status\":\"SUCCESS\",\n" +
      "\"qualityGate\":{\"status\":\"OK\"}\n" +
      "}");

    SonarQubeWebHook.get().addListener(new SonarQubeWebHook.Listener() {

      @Override
      public void onTaskCompleted(String taskId, String taskStatus, @Nullable String qgStatus) {
        eventsPerListener.put("ListenerA", taskId + taskStatus + qgStatus);
      }
    });
    SonarQubeWebHook.get().addListener(new SonarQubeWebHook.Listener() {

      @Override
      public void onTaskCompleted(String taskId, String taskStatus, @Nullable String qgStatus) {
        eventsPerListener.put("ListenerB", taskId + taskStatus + qgStatus);
      }
    });

    jenkins.postJSON("sonarqube-webhook/", "{\n" +
      "\"taskId\":\"AVpBJY0hh5C8Sya1ZSgH\",\n" +
      "\"status\":\"SUCCESS\",\n" +
      "\"qualityGate\":{\"status\":\"OK\"}\n" +
      "}");

    assertThat(eventsPerListener).containsOnly(entry("ListenerA", "AVpBJY0hh5C8Sya1ZSgHSUCCESSOK"),
      entry("ListenerB", "AVpBJY0hh5C8Sya1ZSgHSUCCESSOK"));

    eventsPerListener.clear();

    // No quality gate defined
    jenkins.postJSON("sonarqube-webhook/", "{\n" +
      "\"taskId\":\"AVpBJY0hh5C8Sya1ZSgH\",\n" +
      "\"status\":\"SUCCESS\",\n" +
      "}");

    assertThat(eventsPerListener).containsOnly(entry("ListenerA", "AVpBJY0hh5C8Sya1ZSgHSUCCESSNONE"),
      entry("ListenerB", "AVpBJY0hh5C8Sya1ZSgHSUCCESSNONE"));
  }

}
