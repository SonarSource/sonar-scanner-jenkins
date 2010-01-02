/*
 * Sonar, entreprise quality control tool.
 * Copyright (C) 2007-2008 Hortis-GRC SA
 * mailto:be_agile HAT hortis DOT ch
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package hudson.plugins.sonar;

import hudson.model.Action;
import org.apache.commons.lang.StringUtils;

/**
 * The action appears as the link in the side bar that users will click on in order to go to the Sonar Dashboard.
 *
 * @author Evgeny Mandrikov
 */
public final class SonarAction implements Action {
  private final SonarInstallation sonarInstallation;

  public SonarAction(SonarInstallation sonarInstallation) {
    this.sonarInstallation = sonarInstallation;
  }

  public String getIconFileName() {
    return "/plugin/sonar/images/sonarsource-wave.png";
  }

  public String getDisplayName() {
    return Messages.SonarAction_Sonar(); 
  }

  public String getUrlName() {
    return StringUtils.isEmpty(sonarInstallation.getServerUrl()) ?
        "http://localhost:9000" :
        sonarInstallation.getServerUrl();
  }
}
