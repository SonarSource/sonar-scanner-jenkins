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

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SonarQubeWebHookCrumbExclusionTest {

  private SonarQubeWebHookCrumbExclusion exclusion;
  private HttpServletRequest req;
  private HttpServletResponse resp;
  private FilterChain chain;

  @Before
  public void before() {
    exclusion = new SonarQubeWebHookCrumbExclusion();
    req = mock(HttpServletRequest.class);
    resp = mock(HttpServletResponse.class);
    chain = mock(FilterChain.class);
  }

  @Test
  public void testFullPath() throws Exception {
    when(req.getPathInfo()).thenReturn("/sonarqube-webhook/");
    assertThat(exclusion.process(req, resp, chain)).isTrue();
    verify(chain, times(1)).doFilter(req, resp);
  }

  @Test
  public void testFullPathWithoutSlash() throws Exception {
    when(req.getPathInfo()).thenReturn("/sonarqube-webhook");
    assertThat(exclusion.process(req, resp, chain)).isTrue();
    verify(chain, times(1)).doFilter(req, resp);
  }

  @Test
  public void testInvalidPath() throws Exception {
    when(req.getPathInfo()).thenReturn("/some-other-url/");
    assertThat(exclusion.process(req, resp, chain)).isFalse();
    verify(chain, never()).doFilter(req, resp);
  }

  @Test
  public void testNullPath() throws Exception {
    when(req.getPathInfo()).thenReturn(null);
    assertThat(exclusion.process(req, resp, chain)).isFalse();
    verify(chain, never()).doFilter(req, resp);
  }

  @Test
  public void testEmptyPath() throws Exception {
    when(req.getPathInfo()).thenReturn("");
    assertThat(exclusion.process(req, resp, chain)).isFalse();
    verify(chain, never()).doFilter(req, resp);
  }
}
