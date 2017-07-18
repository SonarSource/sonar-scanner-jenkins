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
package hudson.plugins.sonar;

import hudson.CopyOnWrite;
import hudson.Extension;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.plugins.sonar.SonarPublisher.DescriptorImpl;
import hudson.plugins.sonar.utils.Logger;
import hudson.plugins.sonar.utils.SQServerVersions;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import java.util.List;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Since 2.4
 * The global configuration was migrated from SonarPublisher to this component.
 */
@Extension(ordinal = 100)
public class SonarGlobalConfiguration extends GlobalConfiguration {
  @CopyOnWrite
  private volatile SonarInstallation[] installations = new SonarInstallation[0];
  private volatile boolean buildWrapperEnabled = false;
  private transient boolean migrated = false;

  public SonarGlobalConfiguration() {
    load();
  }

  /**
   * @return all configured {@link hudson.plugins.sonar.SonarInstallation}
   */
  public SonarInstallation[] getInstallations() {
    return installations;
  }

  public boolean isBuildWrapperEnabled() {
    return buildWrapperEnabled;
  }

  public void setInstallations(SonarInstallation... installations) {
    this.installations = installations;
    save();
  }

  public void setBuildWrapperEnabled(boolean enabled) {
    this.buildWrapperEnabled = enabled;
    save();
  }

  /**
   * Attempts to migrated data from SonarPublished, which was previously holding the global configuration.
   * It is thread safe and will refuse to migrate if a SonarQube installation already exists in this class.
   * Migration will only be attempted once. 
   */
  @Initializer(after = InitMilestone.PLUGINS_PREPARED)
  public void migrate() {
    if (migrated) {
      return;
    }

    synchronized (this) {
      if (migrated) {
        return;
      }
      // SonarPublisher might be null if Maven plugin is disabled or not installed
      Jenkins j = Jenkins.getInstance();
      DescriptorImpl publisher = j.getDescriptorByType(SonarPublisher.DescriptorImpl.class);
      if (publisher != null && publisher.getDeprecatedInstallations() != null && publisher.getDeprecatedInstallations().length > 0) {

        if (ArrayUtils.isEmpty(this.installations)) {
          this.installations = publisher.getDeprecatedInstallations();
          this.buildWrapperEnabled = publisher.isDeprecatedBuildWrapperEnabled();
          save();
        } else {
          Logger.LOG.warning("SonarQube server configurations exist in both deprecated SonarPublisher and SonarGlobalConfiguration. Deleting deprecated configuration..");
        }

        publisher.deleteGlobalConfiguration();
      }

      migrated = true;
    }
  }

  @Override
  public boolean configure(StaplerRequest req, JSONObject json) {
    List<SonarInstallation> list = req.bindJSONToList(SonarInstallation.class, json.get("inst"));
    boolean enableBuildWrapper = json.getBoolean("enableBuildWrapper");
    setInstallations(list.toArray(new SonarInstallation[list.size()]));
    setBuildWrapperEnabled(enableBuildWrapper);

    return true;
  }

  public FormValidation doCheckMandatory(@QueryParameter String value) {
    return StringUtils.isBlank(value) ? FormValidation.error(Messages.SonarGlobalConfiguration_MandatoryProperty()) : FormValidation.ok();
  }

  public ListBoxModel doFillServerVersionItems() {
    ListBoxModel items = new ListBoxModel();

    items.add(new Option("5.1 or lower", SQServerVersions.SQ_5_1_OR_LOWER));
    items.add(new Option("5.2", SQServerVersions.SQ_5_2));
    items.add(new Option("5.3 or higher", SQServerVersions.SQ_5_3_OR_HIGHER));
    return items;
  }
}
