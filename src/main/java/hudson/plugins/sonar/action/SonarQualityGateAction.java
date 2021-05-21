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

import hudson.model.InvisibleAction;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * Persists in a build SonarQube quality gate configuration.
 * @author jasper
 */
@ExportedBean(defaultVisibility = 2)
public class SonarQualityGateAction extends InvisibleAction {

    private boolean abortPipeline;

    private String installationName;
    private String serverUrl;
    private String ceTaskId;

    public SonarQualityGateAction(boolean abortPipeline,
                                  String installationName,
                                  String serverUrl,
                                  String ceTaskId) {
        this.abortPipeline = abortPipeline;
        this.installationName = installationName;
        this.serverUrl = serverUrl;
        this.ceTaskId = ceTaskId;
    }

    @Exported
    public boolean isAbortPipeline() {
        return abortPipeline;
    }

    public void setAbortPipeline(boolean abortPipeline) {
        this.abortPipeline = abortPipeline;
    }

    @Exported
    public String getInstallationName() {
        return installationName;
    }

    public void setInstallationName(String installationName) {
        this.installationName = installationName;
    }

    @Exported
    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    @Exported
    public String getCeTaskId() {
        return ceTaskId;
    }

    public void setCeTaskId(String ceTaskId) {
        this.ceTaskId = ceTaskId;
    }

}
