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
import hudson.model.AbstractProject;
import hudson.plugins.sonar.SonarPublisher;
import hudson.plugins.sonar.utils.Logger;
import hudson.tasks.Publisher;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractSonarPublisherSlicerSpec extends UnorderedStringSlicerSpec<AbstractProject<?, ?>> {

  protected abstract String getDefaultValue();

  @Override
  public List getWorkDomain() {
    final List<AbstractProject<?, ?>> workDomain = new ArrayList<AbstractProject<?, ?>>();
    for (final AbstractProject item : Jenkins.getInstance().getItems(AbstractProject.class)) {
      if (getSonarPublisher(item) != null) {
        workDomain.add(item);
      }
    }
    return workDomain;
  }

  @Override
  public String getName(AbstractProject<?, ?> project) {
    return project.getFullName();
  }

  @Override
  public String getDefaultValueString() {
    return getDefaultValue();
  }

  private SonarPublisher getSonarPublisher(final AbstractProject<?, ?> project) {
    for (final Publisher publisher : project.getPublishersList()) {
      if (publisher instanceof SonarPublisher) {
        return (SonarPublisher) publisher;
      }
    }
    return null;
  }

  @Override
  public final List<String> getValues(AbstractProject<?, ?> project) {
    final List<String> values = new ArrayList<String>();
    values.add(doGetValue(getSonarPublisher(project)));
    return values;
  }

  protected abstract String doGetValue(SonarPublisher publisher);

  @Override
  public final boolean setValues(AbstractProject<?, ?> project, List<String> list) {
    if (list.isEmpty()) {
      return false;
    }
    doSetValue(getSonarPublisher(project), list.iterator().next());
    try {
      project.save();
    } catch (IOException e) {
      Logger.LOG.throwing(this.getClass().getName(), "setValues", e);
      return false;
    }
    return true;
  }

  protected abstract void doSetValue(SonarPublisher publisher, String value);

  protected String defaultValueIfBlank(final String value) {
    return StringUtils.isBlank(value) ? getDefaultValue() : value;
  }

  protected String nullIfDefaultValue(final String value) {
    return getDefaultValue().equals(value) ? null : value;
  }
}
