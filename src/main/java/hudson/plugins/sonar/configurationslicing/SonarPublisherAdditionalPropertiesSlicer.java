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
import hudson.model.AbstractProject;
import hudson.plugins.sonar.SonarPublisher;

@Extension(optional = true)
public class SonarPublisherAdditionalPropertiesSlicer extends UnorderedStringSlicer<AbstractProject<?, ?>> {

  public SonarPublisherAdditionalPropertiesSlicer() {
    super(new SonarPublisherAdditionalPropertiesSlicerSpec());
  }

  protected static class SonarPublisherAdditionalPropertiesSlicerSpec extends AbstractSonarPublisherSlicerSpec {

    @Override
    public String getName() {
      return "SonarQube (Post Build) - Additional Properties Slicer";
    }

    @Override
    public String getUrl() {
      return "sqPublisherAdditionalProperties";
    }

    @Override
    protected String doGetValue(SonarPublisher publisher) {
      return defaultValueIfBlank(publisher.getJobAdditionalProperties());
    }

    @Override
    protected void doSetValue(SonarPublisher publisher, String value) {
      publisher.setJobAdditionalProperties(nullIfDefaultValue(value));
    }

    @Override
    protected String getDefaultValue() {
      return "(Empty)";
    }

  }
}
