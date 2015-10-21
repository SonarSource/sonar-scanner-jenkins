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

import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.util.List;

import hudson.Extension;
import hudson.tools.ToolInstallation;
import org.kohsuke.stapler.DataBoundConstructor;
import hudson.tools.DownloadFromUrlInstaller;

public class MsBuildRunnerInstaller extends DownloadFromUrlInstaller {
  @DataBoundConstructor
  public MsBuildRunnerInstaller(String id) {
    super(id);
  }

  @Extension
  public static final class MsBuildRunnerInstallerDescriptorImpl extends DownloadFromUrlInstaller.DescriptorImpl<MsBuildRunnerInstaller> {
    private static final String BASE_URL = "https://github.com/SonarSource/sonar-msbuild-runner/releases/download/";
    private static final String ARTIFACT_NAME = "MSBuild.SonarQube.Runner";
    private static final String NAME = "MSBuild SonarQube Runner";

    @Override
    public String getDisplayName() {
      return Messages.InstallFromGitHub();
    }

    @Override
    public boolean isApplicable(Class<? extends ToolInstallation> toolType) {
      return toolType == MsBuildRunnerInstallation.class;
    }

    @Override
    /** TO REMOVE **/
    public List<? extends Installable> getInstallables() throws IOException {
      return ImmutableList.of(generate("1.0.1"));
    }

    private static Installable generate(String version) {
      StringBuilder builder = new StringBuilder(256);
      builder.append(BASE_URL)
        .append(version).append("/")
        .append(ARTIFACT_NAME).append("-").append(version).append(".zip");

      Installable inst = new Installable();
      inst.url = builder.toString();
      inst.id = version;
      inst.name = NAME + " " + version;

      return inst;
    }

  }
}
