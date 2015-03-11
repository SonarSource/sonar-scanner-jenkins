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
/*
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

import hudson.Extension;
import hudson.tools.DownloadFromUrlInstaller;
import hudson.tools.ToolInstallation;
import org.kohsuke.stapler.DataBoundConstructor;

/**
* Automatic Sonar runner installer from repository.codehaus.org.
*/
public class SonarRunnerInstaller extends DownloadFromUrlInstaller {
  @DataBoundConstructor
  public SonarRunnerInstaller(String id) {
    super(id);
  }

  @Extension
  public static final class DescriptorImpl extends DownloadFromUrlInstaller.DescriptorImpl<SonarRunnerInstaller> {
    @Override
    public String getDisplayName() {
      return Messages.InstallFromMavenCentral();
    }

    @Override
    public boolean isApplicable(Class<? extends ToolInstallation> toolType) {
      return toolType == SonarRunnerInstallation.class;
    }

  }
}
