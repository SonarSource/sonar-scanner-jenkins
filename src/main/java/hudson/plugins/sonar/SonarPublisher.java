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

import hudson.CopyOnWrite;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.maven.AbstractMavenProject;
import hudson.maven.ModuleName;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.JDK;
import hudson.plugins.sonar.model.TriggersConfig;
import hudson.plugins.sonar.utils.Logger;
import hudson.plugins.sonar.utils.MagicNames;
import hudson.plugins.sonar.utils.SonarMaven;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.tasks.Maven;
import hudson.tasks.Maven.MavenInstallation;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Old fields should be left so that old config data can be read in, but
 * they should be deprecated and transient so that they won't show up in XML
 * when writing back
 */
public class SonarPublisher extends Notifier {

  /**
   * Identifies {@link JDK} to be used.
   * Null if no explicit configuration is required.
   *
   * <p>
   * Can't store {@link JDK} directly because {@link Jenkins} and {@link Project}
   * are saved independently.
   *
   * @see Jenkins#getJDK(String)
   */
  private String jdk;

  /**
   * Sonar installation name.
   */
  private final String installationName;

  /**
   * Optional.
   *
   * @since 1.4
   */
  private String branch;

  /**
   * Optional.
   *
   * @since 1.6
   */
  private String language;

  /**
   * Optional.
   */
  private final String mavenOpts;

  /**
   * Optional.
   */
  private final String jobAdditionalProperties;

  /**
   * Triggers. If null, then we should use triggers from {@link SonarInstallation}.
   *
   * @since 1.2
   */
  private TriggersConfig triggers;

  // =================================================
  // Next fields available only for free-style projects

  private String mavenInstallationName;

  /**
   * @since 1.2
   */
  private String rootPom;

  public SonarPublisher(String installationName, String jobAdditionalProperties, String mavenOpts) {
    this(installationName, new TriggersConfig(), jobAdditionalProperties, mavenOpts, null, null);
  }

  public SonarPublisher(
      String installationName,
      TriggersConfig triggers,
      String jobAdditionalProperties, String mavenOpts) {
    this(installationName, null, null, triggers, jobAdditionalProperties, mavenOpts, null, null, null);
  }

  public SonarPublisher(String installationName,
      TriggersConfig triggers,
      String jobAdditionalProperties, String mavenOpts,
      String mavenInstallationName, String rootPom) {
    this(installationName, null, null, triggers, jobAdditionalProperties, mavenOpts, mavenInstallationName, rootPom, null);
  }

  public SonarPublisher(String installationName,
      String branch,
      String language,
      TriggersConfig triggers,
      String jobAdditionalProperties, String mavenOpts,
      String mavenInstallationName, String rootPom) {
    this(installationName, branch, language, triggers, jobAdditionalProperties, mavenOpts, mavenInstallationName, rootPom, null);
  }

  @DataBoundConstructor
  public SonarPublisher(String installationName,
      String branch,
      String language,
      TriggersConfig triggers,
      String jobAdditionalProperties, String mavenOpts,
      String mavenInstallationName, String rootPom, String jdk) {
    super();
    this.installationName = installationName;
    this.branch = branch;
    this.language = language;
    // Triggers
    this.triggers = triggers;
    // Maven
    this.mavenOpts = mavenOpts;
    this.jobAdditionalProperties = jobAdditionalProperties;
    // Non Maven Project
    this.mavenInstallationName = mavenInstallationName;
    this.rootPom = rootPom;
    this.jdk = jdk;
  }

  /**
   * Gets the JDK that this Sonar publisher is configured with, or null.
   */
  public JDK getJDK() {
    return Hudson.getInstance().getJDK(jdk);
  }

  /**
   * @return name of {@link hudson.plugins.sonar.SonarInstallation}
   */
  public String getInstallationName() {
    return installationName;
  }

  /**
   * @return MAVEN_OPTS
   */
  public String getMavenOpts() {
    return mavenOpts;
  }

  /**
   * @return additional Maven options like "-Pprofile" and "-Dname=value"
   */
  public String getJobAdditionalProperties() {
    return StringUtils.trimToEmpty(jobAdditionalProperties);
  }

  /**
   * @return true, if we should use triggers from {@link SonarInstallation}
   */
  public boolean isUseGlobalTriggers() {
    return triggers == null;
  }

  public boolean isUseLocalTriggers() {
    return !isUseGlobalTriggers();
  }

  /**
   * See <a href="http://docs.codehaus.org/display/SONAR/Advanced+parameters#Advancedparameters-ManageSCMbranches">Sonar Branch option</a>.
   *
   * @return branch
   * @since 1.4
   */
  public String getBranch() {
    return branch;
  }

  public String getLanguage() {
    return StringUtils.trimToEmpty(language);
  }

  /**
   * @return triggers configuration
   */
  public TriggersConfig getTriggers() {
    return triggers;
  }

  /**
   * @return name of {@link hudson.tasks.Maven.MavenInstallation}
   */
  public String getMavenInstallationName() {
    return mavenInstallationName;
  }

  /**
   * Root POM. Should be applied only for free-style projects.
   *
   * @return Root POM
   */
  public String getRootPom() {
    return StringUtils.trimToEmpty(rootPom);
  }

  public static boolean isMavenBuilder(AbstractProject<?, ?> currentProject) {
    return currentProject instanceof MavenModuleSet;
  }

  public SonarInstallation getInstallation() {
    return SonarInstallation.get(getInstallationName());
  }

  private boolean isSkip(AbstractBuild<?, ?> build, BuildListener listener, SonarInstallation sonarInstallation) throws IOException, InterruptedException {
    String skipLaunchMsg;
    if (isUseGlobalTriggers()) {
      skipLaunchMsg = sonarInstallation.getTriggers().isSkipSonar(build, listener);
    } else {
      skipLaunchMsg = getTriggers().isSkipSonar(build, listener);
    }
    if (skipLaunchMsg != null) {
      listener.getLogger().println(skipLaunchMsg);
      return true;
    }
    return false;
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
    if (!SonarRunnerBuilder.isSonarInstallationValid(getInstallationName(), listener)) {
      return false;
    }
    SonarInstallation sonarInstallation = getInstallation();

    if (isSkip(build, listener, sonarInstallation)) {
      return true;
    }
    build.addAction(new BuildSonarAction());

    boolean sonarSuccess = executeSonar(build, launcher, listener, sonarInstallation);
    if (!sonarSuccess) {
      // returning false has no effect on the global build status so need to do it manually
      build.setResult(Result.FAILURE);
    }
    listener.getLogger().println("Sonar analysis completed: " + build.getResult());
    return sonarSuccess;
  }

  public MavenModuleSet getMavenProject(AbstractBuild<?, ?> build) {
    return (build.getProject() instanceof MavenModuleSet) ? (MavenModuleSet) build.getProject() : null;
  }

  private String getPomName(AbstractBuild<?, ?> build) {
    String pomName;
    MavenModuleSet mavenModuleProject = getMavenProject(build);
    if (mavenModuleProject != null) {
      pomName = mavenModuleProject.getRootPOM();
    } else {
      pomName = getRootPom();
    }
    if (StringUtils.isEmpty(pomName)) {
      pomName = "pom.xml";
    }
    return pomName;
  }

  private boolean executeSonar(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, SonarInstallation sonarInstallation) {
    try {
      String pomName = getPomName(build);
      String mavenInstallName = getMavenInstallationName();
      if (isMavenBuilder(build.getProject())) {
        MavenModuleSet mavenModuleSet = getMavenProject(build);
        if (null != mavenModuleSet.getMaven().getName()) {
          mavenInstallName = mavenModuleSet.getMaven().getName();
        }
      }

      // Execute maven
      return SonarMaven.executeMaven(build, launcher, listener, mavenInstallName, pomName, sonarInstallation, this, getJDK());
    } catch (IOException e) {
      Logger.printFailureMessage(listener);
      Util.displayIOException(e, listener);
      e.printStackTrace(listener.fatalError("command execution failed"));
      return false;
    } catch (InterruptedException e) {
      return false;
    } catch (Exception e) {
      Logger.printFailureMessage(listener);
      e.printStackTrace(listener.fatalError("command execution failed"));
      return false;
    }
  }

  protected String getSonarUrl(AbstractProject<?, ?> project) {
    SonarInstallation sonarInstallation = getInstallation();
    if (sonarInstallation == null) {
      return null;
    }
    String url = sonarInstallation.getServerLink();
    if (project instanceof AbstractMavenProject) {
      // Maven Project
      AbstractMavenProject<?, ?> mavenProject = (AbstractMavenProject<?, ?>) project;
      if (mavenProject.getRootProject() instanceof MavenModuleSet) {
        MavenModuleSet mms = (MavenModuleSet) mavenProject.getRootProject();
        MavenModule rootModule = mms.getRootModule();
        if (rootModule != null) {
          ModuleName moduleName = rootModule.getModuleName();
          url = sonarInstallation.getProjectLink(moduleName.groupId, moduleName.artifactId, getBranch());
        }
      }
    }
    /**
     * Free-style job:
     * If project was built by maven, then pom.xml already exists
     * If project wasn't built by maven, then there is should be generated pom.xml
     */
    try {
      AbstractBuild<?, ?> lastBuild = project.getLastBuild();
      if (lastBuild != null) {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new InputStreamReader(lastBuild.getWorkspace().child(getPomName(lastBuild)).read()));
        String groupId = model.getGroupId();
        String artifactId = model.getArtifactId();
        url = sonarInstallation.getProjectLink(groupId, artifactId, getBranch());
      }
    } catch (IOException e) {
      // ignore
    } catch (XmlPullParserException e) {
      // ignore
    } catch (NullPointerException e) {
      // ignore something in the line can be null for maven project
    }
    return url;
  }

  @Override
  public Action getProjectAction(AbstractProject<?, ?> project) {
    return new ProjectSonarAction(getSonarUrl(project));
  }

  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.BUILD;
  }

  @Extension(ordinal = 1000)
  public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

    @CopyOnWrite
    private volatile SonarInstallation[] installations = new SonarInstallation[0];

    public DescriptorImpl() {
      super();
      load();
    }

    @Override
    public String getDisplayName() {
      return "Sonar";
    }

    @Override
    public String getHelpFile() {
      return MagicNames.PLUGIN_HOME + "/help.html";
    }

    /**
     * @return all configured {@link hudson.plugins.sonar.SonarInstallation}
     */
    public SonarInstallation[] getInstallations() {
      return installations;
    }

    public void setInstallations(SonarInstallation... installations) {
      this.installations = installations;
      save();
    }

    /**
     * This method is used in UI, so signature and location of this method is important (see SONARPLUGINS-1337).
     *
     * @return all configured {@link hudson.tasks.Maven.MavenInstallation}
     */
    public MavenInstallation[] getMavenInstallations() {
      return Hudson.getInstance().getDescriptorByType(Maven.DescriptorImpl.class).getInstallations();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) {
      List<SonarInstallation> list = req.bindJSONToList(SonarInstallation.class, json.get("inst"));
      setInstallations(list.toArray(new SonarInstallation[list.size()]));
      return true;
    }

    public FormValidation doCheckMandatory(@QueryParameter String value) {
      return StringUtils.isBlank(value) ?
          FormValidation.error(Messages.SonarPublisher_MandatoryProperty()) : FormValidation.ok();
    }

    public FormValidation doCheckMandatoryAndNoSpaces(@QueryParameter String value) {
      return (StringUtils.isBlank(value) || value.contains(" ")) ?
          FormValidation.error(Messages.SonarPublisher_MandatoryPropertySpaces()) : FormValidation.ok();
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
      // eventually check if job type of FreeStyleProject.class || MavenModuleSet.class
      return true;
    }
  }
}
