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
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author drautureau
 */
public abstract class SonarPublisherSlicerSpec extends UnorderedStringSlicerSpec<MavenModuleSet> {
    private static final String DEFAULT = "(Empty)";

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
        return DEFAULT;
    }

    protected SonarPublisher getSonarPublisher(final MavenModuleSet project) {
        for (final Publisher publisher : project.getPublishersList()) {
            if (publisher instanceof SonarPublisher) {
                return (SonarPublisher)publisher;
            }
        }
        return null;
    }

    protected String defaultValueIfBlank(final String value) {
        return StringUtils.isBlank(value) ? DEFAULT : value;
    }

    protected String nullIfDefaultValue(final String value) {
        return DEFAULT.equals(value) ? null : value;
    }
}
