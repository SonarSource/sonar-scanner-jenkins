/*
 * SonarQube Scanner for Jenkins
 * Copyright (C) 2007-2021 SonarSource SA
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
package hudson.plugins.sonar.action;

import hudson.model.Run;
import hudson.plugins.sonar.client.ProjectInformation;
import hudson.plugins.sonar.client.SQProjectResolver;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;

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
    SonarAnalysisAction analysis = new SonarAnalysisAction("inst", "credId", null);
    analysis.setCeTaskId("taskId");
    analysis.setUrl("projUrl");
    analysis.setServerUrl("serverUrl");
    analysis.setUrl("projUrl");
    Run<?, ?> run = mock(Run.class);

    cache.get(resolver, 0, Collections.singletonList(analysis), run);
    verify(resolver).resolve("serverUrl", "projUrl", "taskId", "inst", run);
  }

  @Test
  public void testResolveUsingInstallationUrl() {
    SonarAnalysisAction analysis = new SonarAnalysisAction("inst", "credId", "installationUrl");
    analysis.setCeTaskId("taskId");
    analysis.setUrl("projUrl");
    analysis.setServerUrl("serverUrl");
    analysis.setUrl("projUrl");
    Run<?, ?> run = mock(Run.class);

    cache.get(resolver, 0, Collections.singletonList(analysis), run);
    verify(resolver).resolve("installationUrl", "projUrl", "taskId", "inst", run);
  }

  @Test
  public void testResponseCached() {
    ProjectInformation mocked1 = createProj(now(), "success");
    ProjectInformation mocked2 = createProj(now(), "error");
    SonarAnalysisAction analysis1 = createAnalysis("serverUrl", "projUrl", "taskId1");
    SonarAnalysisAction analysis2 = createAnalysis("serverUrl", "projUrl", "taskId2");
    Run<?, ?> run = mock(Run.class);

    when(resolver.resolve(analysis1.getServerUrl(), analysis1.getUrl(), Objects.requireNonNull(analysis1.getCeTaskId()), analysis1.getInstallationName(), run)).thenReturn(mocked1);
    when(resolver.resolve(analysis2.getServerUrl(), analysis2.getUrl(), Objects.requireNonNull(analysis2.getCeTaskId()), analysis2.getInstallationName(), run)).thenReturn(mocked2);

    List<ProjectInformation> projs = cache.get(resolver, 0, Collections.singletonList(analysis1), run);
    // Calling it again in quick succession, even with more analyses, should still return the cached value.
    List<ProjectInformation> projs2 = cache.get(resolver, 0, Arrays.asList(analysis1, analysis2), run);

    assertThat(projs).hasSize(1);
    assertThat(projs2).hasSize(1);
    assertThat(projs.get(0).getCeStatus()).isEqualTo("success");
    verify(resolver, times(1)).resolve(analysis1.getServerUrl(), analysis1.getUrl(), Objects.requireNonNull(analysis1.getCeTaskId()), analysis1.getInstallationName(), run);
    verify(resolver, times(0)).resolve(analysis2.getServerUrl(), analysis2.getUrl(), Objects.requireNonNull(analysis2.getCeTaskId()), analysis2.getInstallationName(), run);

    // Invalidate the cache, by setting the age to more than 30s.
    cache.cacheProjectInfo(projs, System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(40));

    // Calling it again with more analyses, now that the cache is invalid, should change the result.
    List<ProjectInformation> projs3 = cache.get(resolver, 0, Arrays.asList(analysis1, analysis2), run);

    assertThat(projs3).hasSize(2);
    assertThat(projs3.get(1).getCeStatus()).isEqualTo("error");
    verify(resolver, times(1)).resolve(analysis2.getServerUrl(), analysis2.getUrl(), Objects.requireNonNull(analysis2.getCeTaskId()), analysis2.getInstallationName(), run);
  }

  @Test
  public void testCacheWithCE() {
    ProjectInformation proj = createProj(now(), "success");
    SonarAnalysisAction analysis = createAnalysis("serverUrl", "projUrl1", "taskId");
    Run<?, ?> run = mock(Run.class);
    when(resolver.resolve("serverUrl", "projUrl1", "taskId", "inst", run)).thenReturn(proj);

    ProjectInformation info1 = cache.get(resolver, 0, analysis, run);
    assertThat(info1).isNotNull();
    assertThat(info1.getCeStatus()).isEqualTo("success");
    assertThat(info1.getStatus()).isEqualTo("OK");

    ProjectInformation info2 = cache.get(resolver, 0, analysis, run);

    assertThat(info1).isEqualTo(info2);
    verify(resolver, times(1)).resolve("serverUrl", "projUrl1", "taskId", "inst", run);
  }

  private SonarAnalysisAction createAnalysis(String serverUrl, String url, String taskId) {
    SonarAnalysisAction analysis = new SonarAnalysisAction("inst", "credId", null);
    analysis.setServerUrl(serverUrl);
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
