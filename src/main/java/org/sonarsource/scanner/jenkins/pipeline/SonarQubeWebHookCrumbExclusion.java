/*
 * SonarQube Scanner for Jenkins
 * Copyright (C) 2007-2025 SonarSource SA
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

import hudson.Extension;
import hudson.security.csrf.CrumbExclusion;
import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.apache.commons.lang3.StringUtils.isEmpty;

@Extension
public class SonarQubeWebHookCrumbExclusion extends CrumbExclusion {

  @Override
  public boolean process(HttpServletRequest req, HttpServletResponse resp, FilterChain chain) throws IOException, ServletException {
    String pathInfo = req.getPathInfo();
    if (isEmpty(pathInfo)) {
      return false;
    }
    pathInfo = pathInfo.endsWith("/") ? pathInfo : (pathInfo + '/');
    if (!pathInfo.equals(getExclusionPath())) {
      return false;
    }
    chain.doFilter(req, resp);
    return true;
  }

  public String getExclusionPath() {
    return "/" + SonarQubeWebHook.URLNAME + "/";
  }
}
