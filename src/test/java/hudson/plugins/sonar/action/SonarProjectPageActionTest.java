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
import hudson.plugins.sonar.client.QualityGateResolver;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class SonarProjectPageActionTest {
  private QualityGateResolver qgResolver;
  
  @Before
  public void setUp() {
    qgResolver = mock(QualityGateResolver.class);
    ProjectInformation proj = mock(ProjectInformation.class);
    when(qgResolver.get(anyString(), anyString())).thenReturn(proj);
  }
  
  @Test
  public void test() {
    SonarAnalysisAction[] analyses = {
      createAnalysis("inst1", "url1"),
      createAnalysis("inst2", "url2")
    };
    
    SonarProjectPageAction projectPage = new SonarProjectPageAction(Arrays.asList(analyses), qgResolver);
    assertThat(projectPage.getProjects()).hasSize(2);
    
    verify(qgResolver).get("url1", "inst1");
    verify(qgResolver).get("url2", "inst2");
    verifyNoMoreInteractions(qgResolver);
  }
  
  private static SonarAnalysisAction createAnalysis(String instName, String url) {
    SonarAnalysisAction analysis = new SonarAnalysisAction(instName);
    analysis.setUrl(url);
    return analysis;
  }
}
