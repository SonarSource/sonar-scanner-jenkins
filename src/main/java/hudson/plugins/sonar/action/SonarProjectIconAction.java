/*
 * SonarQube Scanner for Jenkins
 * Copyright (C) 2007-2019 SonarSource SA
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

import hudson.PluginWrapper;
import hudson.model.ProminentProjectAction;
import hudson.plugins.sonar.Messages;
import hudson.plugins.sonar.SonarPlugin;
import jenkins.model.Jenkins;

/**
 * {@link ProminentProjectAction} that allows user to go to the Sonar Dashboard.
 *
 * @author Evgeny Mandrikov
 * @since 1.2
 */
public final class SonarProjectIconAction implements ProminentProjectAction {
  private final SonarAnalysisAction buildInfo;

  public SonarProjectIconAction() {
    this.buildInfo = null;
  }

  public SonarProjectIconAction(SonarAnalysisAction buildInfo) {
    this.buildInfo = buildInfo;
  }

  @Override
  public String getIconFileName() {
    PluginWrapper wrapper = Jenkins.getInstanceOrNull().getPluginManager()
      .getPlugin(SonarPlugin.class);
    return "/plugin/" + wrapper.getShortName() + "/images/waves_48x48.png";
  }

  @Override
  public String getDisplayName() {
    return Messages.SonarAction_Sonar();
  }

  @Override
  public String getUrlName() {
    return buildInfo != null ? buildInfo.getUrl() : null;
  }

  public SonarAnalysisAction getBuildInfo() {
    return buildInfo;
  }
}
