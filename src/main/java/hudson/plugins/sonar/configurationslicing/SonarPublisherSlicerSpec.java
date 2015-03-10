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

import configurationslicing.UnorderedStringSlicer.UnorderedStringSlicerSpec;
import hudson.maven.MavenModuleSet;
import hudson.plugins.sonar.SonarPublisher;
import hudson.tasks.Publisher;
import jenkins.model.Jenkins;

import java.util.ArrayList;
import java.util.List;

public abstract class SonarPublisherSlicerSpec extends UnorderedStringSlicerSpec<MavenModuleSet> {

  protected abstract String getDefaultValue();

  @Override
  public List getWorkDomain() {
    final List<MavenModuleSet> workDomain = new ArrayList<MavenModuleSet>();
    for (final MavenModuleSet item : Jenkins.getInstance().getItems(MavenModuleSet.class)) {
      if (getSonarPublisher(item) != null) {
        workDomain.add(item);
      }
    }
    return workDomain;
  }

  @Override
  public String getName(MavenModuleSet mavenModuleSet) {
    return mavenModuleSet.getFullName();
  }

  @Override
  public String getDefaultValueString() {
    return getDefaultValue();
  }

  protected SonarPublisher getSonarPublisher(final MavenModuleSet project) {
    for (final Publisher publisher : project.getPublishersList()) {
      if (publisher instanceof SonarPublisher) {
        return (SonarPublisher) publisher;
      }
    }
    return null;
  }
}
