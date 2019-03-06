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
package hudson.plugins.sonar.configurationslicing;

import configurationslicing.UnorderedStringSlicer.UnorderedStringSlicerSpec;
import hudson.model.AbstractProject;
import hudson.plugins.sonar.SonarPublisher;
import hudson.plugins.sonar.utils.Logger;
import hudson.tasks.Publisher;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;

public abstract class AbstractSonarPublisherSlicerSpec extends UnorderedStringSlicerSpec<AbstractProject<?, ?>> {

  protected abstract String getDefaultValue();

  @Override
  public List<AbstractProject<?, ?>> getWorkDomain() {
    final List<AbstractProject<?, ?>> workDomain = new ArrayList<>();
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

  private static SonarPublisher getSonarPublisher(final AbstractProject<?, ?> project) {
    for (final Publisher publisher : project.getPublishersList()) {
      if (publisher instanceof SonarPublisher) {
        return (SonarPublisher) publisher;
      }
    }
    return null;
  }

  @Override
  public final List<String> getValues(AbstractProject<?, ?> project) {
    final List<String> values = new ArrayList<>();
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
