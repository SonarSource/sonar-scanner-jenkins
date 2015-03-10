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
package hudson.plugins.sonar.configurationslicing;

import configurationslicing.UnorderedStringSlicer;
import hudson.Extension;
import hudson.maven.MavenModuleSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Extension
public class JdkSlicer extends UnorderedStringSlicer<MavenModuleSet> {

  public JdkSlicer() {
    super(new JdkSlicerSpec());
  }

  protected static class JdkSlicerSpec extends SonarPublisherSlicerSpec {

    @Override
    public String getName() {
      return "SonarQube JDK";
    }

    @Override
    public String getUrl() {
      return "sonarqubejdk";
    }

    @Override
    public List<String> getValues(MavenModuleSet mavenModuleSet) {
      final List<String> values = new ArrayList<String>();
      final String jdk = getSonarPublisher(mavenModuleSet).getJdk();
      values.add(jdk);
      return values;
    }

    @Override
    public boolean setValues(MavenModuleSet mavenModuleSet, List<String> list) {
      if (list.isEmpty()) {
        return false;
      }
      getSonarPublisher(mavenModuleSet).setJdk(list.iterator().next());
      try {
        mavenModuleSet.save();
      } catch (IOException e) {
        return false;
      }
      return true;
    }

    @Override
    protected String getDefaultValue() {
      return "(Inherit From Job)";
    }
  }
}
