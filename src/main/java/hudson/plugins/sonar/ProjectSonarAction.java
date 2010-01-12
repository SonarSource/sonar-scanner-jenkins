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
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.maven.ModuleName;
import hudson.model.AbstractProject;

/**
 * The action appears as the link in the side bar that users will click on in order to go to the Sonar Dashboard.
 *
 * @author Evgeny Mandrikov
 * @since 1.2
 */
public final class ProjectSonarAction extends SonarAction {
  private final AbstractProject<?, ?> project;

  public ProjectSonarAction(AbstractProject<?, ?> project) {
    this.project = project;
  }

  @Override
  public String getUrlName() {
    final SonarPublisher publisher = project.getPublishersList().get(SonarPublisher.class);
    final SonarInstallation sonarInstallation = publisher.getInstallation();
    if (sonarInstallation == null) {
      return null;
    }

    if (project instanceof AbstractMavenProject) {
      // Maven Project
      AbstractMavenProject mavenProject = (AbstractMavenProject) project;
      if (mavenProject.getRootProject() instanceof MavenModuleSet) {
        MavenModuleSet mms = (MavenModuleSet) mavenProject.getRootProject();
        MavenModule rootModule = mms.getRootModule();
        if (rootModule != null) {
          ModuleName moduleName = rootModule.getModuleName();
          return sonarInstallation.getProjectLink(
              moduleName.groupId,
              moduleName.artifactId
          );
        }
      }
    } else {
      // FIXME Non-Maven Project
//      return sonarInstallation.getProjectLink(
//          publisher.getProject().getGroupId(),
//          publisher.getProject().getArtifactId()
//      );
    }
    return sonarInstallation.getServerLink();
  }
}
