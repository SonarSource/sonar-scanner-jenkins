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

import hudson.maven.AbstractMavenProject;
import hudson.maven.MavenModuleSet;
import hudson.maven.ModuleName;
import hudson.model.AbstractProject;

/**
 * The action appears as the link in the side bar that users will click on in order to go to the Sonar Dashboard.
 *
 * @author Evgeny Mandrikov
 */
public final class ProjectSonarAction extends SonarAction {
  private final AbstractProject<?, ?> project;
  private final SonarInstallation sonarInstallation;

  public ProjectSonarAction(AbstractProject<?, ?> project, SonarInstallation sonarInstallation) {
    this.project = project;
    this.sonarInstallation = sonarInstallation;
  }

  @Override
  public String getUrlName() {
    if (project instanceof AbstractMavenProject) {
      AbstractMavenProject mavenProject = (AbstractMavenProject) project;
      if (mavenProject.getRootProject() instanceof MavenModuleSet) {
        MavenModuleSet mms = (MavenModuleSet) mavenProject.getRootProject();
        ModuleName moduleName = mms.getRootModule().getModuleName();
        return sonarInstallation.getProjectLink(
            moduleName.groupId,
            moduleName.artifactId
        );
      }
    }
    // TODO sonarLight
    return sonarInstallation.getServerLink();
  }
}
