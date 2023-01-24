/*
 * SonarQube Scanner for Jenkins
 * Copyright (C) 2007-2023 SonarSource SA
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

import configurationslicing.UnorderedStringSlicer;
import hudson.Extension;
import hudson.model.Project;
import hudson.plugins.sonar.SonarRunnerBuilder;

@Extension(optional = true)
public class SonarRunnerBuilderJdkSlicer extends UnorderedStringSlicer<Project<?, ?>> {

  public SonarRunnerBuilderJdkSlicer() {
    super(new SonarRunnerBuilderJdkSlicerSpec());
  }

  protected static class SonarRunnerBuilderJdkSlicerSpec extends AbstractSonarRunnerBuilderSlicerSpec {

    @Override
    public String getName() {
      return "SonarQube (Build Step) - JDK Slicer";
    }

    @Override
    public String getUrl() {
      return "sqRunnerBuilderJdk";
    }

    @Override
    protected String doGetValue(SonarRunnerBuilder builder) {
      return defaultValueIfBlank(builder.getJdk());
    }

    @Override
    protected void doSetValue(SonarRunnerBuilder builder, String value) {
      builder.setJdk(nullIfDefaultValue(value));
    }

    @Override
    protected String getDefaultValue() {
      return "(Inherit From Job)";
    }
  }
}
