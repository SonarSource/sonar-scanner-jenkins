package hudson.plugins.sonar;

import hudson.model.Action;
import org.apache.commons.lang.StringUtils;

/**
 * The action appears as the link in the side bar that users will click on in order to go to the Sonar Dashboard.
 *
 * @author Evgeny Mandrikov
 */
public final class SonarAction implements Action {
  private final SonarInstallation sonarInstallation;

  public SonarAction(SonarInstallation sonarInstallation) {
    this.sonarInstallation = sonarInstallation;
  }

  public String getIconFileName() {
    return "/plugin/sonar/images/sonarsource-wave.png";
  }

  public String getDisplayName() {
    return Messages.SonarAction_Sonar(); 
  }

  public String getUrlName() {
    return StringUtils.isEmpty(sonarInstallation.getServerUrl()) ?
        "http://localhost:9000" :
        sonarInstallation.getServerUrl();
  }
}
