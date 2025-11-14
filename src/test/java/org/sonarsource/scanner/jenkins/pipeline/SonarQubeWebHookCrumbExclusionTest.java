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

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SonarQubeWebHookCrumbExclusionTest {

  private SonarQubeWebHookCrumbExclusion exclusion;
  private HttpServletRequest req;
  private HttpServletResponse resp;
  private FilterChain chain;

  @BeforeEach
  void setUp() {
    exclusion = new SonarQubeWebHookCrumbExclusion();
    req = mock(HttpServletRequest.class);
    resp = mock(HttpServletResponse.class);
    chain = mock(FilterChain.class);
  }

  @Test
  void testFullPath() throws Exception {
    when(req.getPathInfo()).thenReturn("/sonarqube-webhook/");
    assertThat(exclusion.process(req, resp, chain)).isTrue();
    verify(chain, times(1)).doFilter(req, resp);
  }

  @Test
  void testFullPathWithoutSlash() throws Exception {
    when(req.getPathInfo()).thenReturn("/sonarqube-webhook");
    assertThat(exclusion.process(req, resp, chain)).isTrue();
    verify(chain, times(1)).doFilter(req, resp);
  }

  @Test
  void testInvalidPath() throws Exception {
    when(req.getPathInfo()).thenReturn("/some-other-url/");
    assertThat(exclusion.process(req, resp, chain)).isFalse();
    verify(chain, never()).doFilter(req, resp);
  }

  @Test
  void testNullPath() throws Exception {
    when(req.getPathInfo()).thenReturn(null);
    assertThat(exclusion.process(req, resp, chain)).isFalse();
    verify(chain, never()).doFilter(req, resp);
  }

  @Test
  void testEmptyPath() throws Exception {
    when(req.getPathInfo()).thenReturn("");
    assertThat(exclusion.process(req, resp, chain)).isFalse();
    verify(chain, never()).doFilter(req, resp);
  }
}
