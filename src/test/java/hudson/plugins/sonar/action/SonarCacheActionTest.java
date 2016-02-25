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
package hudson.plugins.sonar.action;

import hudson.plugins.sonar.client.ProjectInformation;
import hudson.plugins.sonar.client.SQProjectResolver;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SonarCacheActionTest {
  private SonarCacheAction cache;
  private SQProjectResolver resolver;

  @Before
  public void setUp() {
    resolver = mock(SQProjectResolver.class);
    cache = new SonarCacheAction();
  }

  @Test
  public void testCacheInvalidation() {
    // task done -> always valid
    assertThat(SonarCacheAction.isEntryValid(createProj(0, "success"), 0)).isTrue();
    assertThat(SonarCacheAction.isEntryValid(createProj(0, "failed"), 0)).isTrue();
    assertThat(SonarCacheAction.isEntryValid(createProj(0, "canceled"), 0)).isTrue();

    // task not done -> not valid
    assertThat(SonarCacheAction.isEntryValid(createProj(0, "pending"), 0)).isFalse();

    // no ce task -> depends on time of last update and on time of last build
    assertThat(SonarCacheAction.isEntryValid(createProj(now(), null), now())).isFalse();
    assertThat(SonarCacheAction.isEntryValid(createProj(now(), null), now() - tenMinutes())).isTrue();
    assertThat(SonarCacheAction.isEntryValid(createProj(now() - tenMinutes(), null), now() - 2 * tenMinutes())).isTrue();

    assertThat(SonarCacheAction.isEntryValid(createProj(now() - tenMinutes(), null), now() - tenMinutes())).isFalse();
    assertThat(SonarCacheAction.isEntryValid(createProj(now() - tenMinutes(), null), now())).isFalse();
  }

  @Test
  public void testResolve() {
    SonarAnalysisAction analysis = new SonarAnalysisAction("inst");
    analysis.setCeTaskId("taskId");
    analysis.setUrl("projUrl");

    cache.get(resolver, 0, Collections.singletonList(analysis));
    verify(resolver).resolve("projUrl", "taskId", "inst");
  }

  @Test
  public void testResponseCached() {
    ProjectInformation mocked = createProj(now(), "success");
    SonarAnalysisAction analysis = createAnalysis("projUrl", "taskId");

    when(resolver.resolve("projUrl", "taskId", "inst")).thenReturn(mocked);
    List<ProjectInformation> projs = cache.get(resolver, 0, Collections.singletonList(analysis));
    List<ProjectInformation> projs2 = cache.get(resolver, 0, Collections.singletonList(analysis));

    assertThat(projs).isEqualTo(projs2);
    assertThat(projs).hasSize(1);
    assertThat(projs.get(0).getCeStatus()).isEqualTo("success");
    verify(resolver, times(1)).resolve("projUrl", "taskId", "inst");
  }

  @Test
  public void testCacheWithCE() {
    ProjectInformation proj = createProj(now(), "success");
    SonarAnalysisAction analysis = createAnalysis("projUrl1", "taskId");
    when(resolver.resolve("projUrl1", "taskId", "inst")).thenReturn(proj);

    ProjectInformation info1 = cache.get(resolver, 0, analysis);
    assertThat(info1).isNotNull();
    assertThat(info1.getCeStatus()).isEqualTo("success");
    assertThat(info1.getStatus()).isEqualTo("OK");

    ProjectInformation info2 = cache.get(resolver, 0, analysis);

    assertThat(info1).isEqualTo(info2);
    verify(resolver, times(1)).resolve("projUrl1", "taskId", "inst");
  }

  @Test
  public void testCacheWithoutCE() {
    ProjectInformation proj = createProj(now(), null);
    SonarAnalysisAction analysis = createAnalysis("projUrl2", null);
    when(resolver.resolve("projUrl2", null, "inst")).thenReturn(proj);

    ProjectInformation info1 = cache.get(resolver, 0, analysis);
    assertThat(info1).isNotNull();
    assertThat(info1.getCeStatus()).isNull();
    assertThat(info1.getStatus()).isEqualTo("OK");

    ProjectInformation info2 = cache.get(resolver, 0, analysis);
    assertThat(info1).isEqualTo(info2);

    // build finished just now -> invalidate cache
    ProjectInformation info3 = cache.get(resolver, now(), analysis);
    assertThat(info1).isEqualTo(info3);

    verify(resolver, times(2)).resolve("projUrl2", null, "inst");
  }

  private SonarAnalysisAction createAnalysis(String url, String taskId) {
    SonarAnalysisAction analysis = new SonarAnalysisAction("inst");
    analysis.setCeTaskId(taskId);
    analysis.setUrl(url);
    return analysis;
  }

  private static long now() {
    return System.currentTimeMillis();
  }

  private static long tenMinutes() {
    return 1000 * 10 * 60;
  }

  private static ProjectInformation createProj(long creationTime, String ceTaskStatus) {
    ProjectInformation proj = mock(ProjectInformation.class);
    when(proj.created()).thenReturn(creationTime);
    when(proj.getCeStatus()).thenReturn(ceTaskStatus);
    when(proj.getStatus()).thenReturn("OK");
    return proj;
  }
}
