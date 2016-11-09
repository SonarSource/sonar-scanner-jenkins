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

import hudson.model.Action;
import hudson.model.Run;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SonarBuildBadgeActionFactoryTest {
  private SonarBuildBadgeActionFactory factory;

  @Before
  public void setUp() {
    factory = new SonarBuildBadgeActionFactory();
  }

  @Test
  public void testNoBadgeIfNoSonar() {
    Run r = mock(Run.class);
    when(r.getActions()).thenReturn(Collections.<Action>emptyList());
    Collection<? extends Action> badges = factory.createFor(r);
    assertThat(badges).isEmpty();
  }

  @Test
  public void testUrl() {
    Run r = mock(Run.class);
    when(r.getActions()).thenReturn(Collections.<Action>singletonList(createBuildInfo("http://myserver/myproject")));
    Collection<? extends Action> badges = factory.createFor(r);
    assertBadge(badges, "http://myserver/myproject");
  }

  @Test
  public void testMultipleAnalysis() {
    Run r = mock(Run.class);
    SonarAnalysisAction[] actions = {
      createBuildInfo("http://myserver/myproject1"),
      createBuildInfo("http://myserver/myproject2")
    };
    when(r.getActions()).thenReturn(Arrays.<Action>asList(actions));
    Collection<? extends Action> badges = factory.createFor(r);
    assertBadge(badges, null);
  }

  @Test
  public void testNoUrl() {
    Run r = mock(Run.class);
    when(r.getActions()).thenReturn(Collections.<Action>singletonList(createBuildInfo(null)));
    Collection<? extends Action> badges = factory.createFor(r);
    assertBadge(badges, null);
  }

  private static void assertBadge(Collection<? extends Action> actions, String url) {
    assertThat(actions).hasSize(1);
    Action action = actions.iterator().next();
    assertThat(action).isExactlyInstanceOf(SonarBuildBadgeAction.class);
    SonarBuildBadgeAction badge = (SonarBuildBadgeAction) action;
    assertThat(badge.getUrl()).isEqualTo(url);
  }

  private static SonarAnalysisAction createBuildInfo(String url) {
    SonarAnalysisAction buildInfo = new SonarAnalysisAction("my sonar");
    buildInfo.setUrl(url);
    return buildInfo;
  }
}
