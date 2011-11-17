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

import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Cause.UserCause;
import hudson.model.CauseAction;
import hudson.model.Item;
import hudson.plugins.sonar.model.TriggersConfig.SonarCause;
import hudson.plugins.sonar.utils.MagicNames;
import java.io.IOException;
import javax.servlet.ServletException;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Initiate a build with the SonarAction as the cause
 * 
 * @author Peter Hayes
 */
public class BuildWithSonarAction implements Action {
  private AbstractProject<?, ?> project;
  
  public BuildWithSonarAction(AbstractProject<?, ?> project) {
    this.project = project;
  }

  public AbstractProject<?, ?> getProject() {
    return project;
  }
  
  public String getIconFileName() {
    // Make sure user has build permission    
    return project.hasPermission(Item.BUILD) ? MagicNames.BUILD_WITH_SONAR_ICON : null;
  }

  public String getDisplayName() {
    return Messages.BuildWithSonarAction_Sonar();
  }

  public String getUrlName() {
    return "buildWithSonar";
  }
  
  public void doSubmit(StaplerRequest req, StaplerResponse resp) throws IOException, ServletException {
    // Make sure logged in user has permission if security is enabled
    project.checkPermission(Item.BUILD);
    
    // Schedule the build now
    project.scheduleBuild(0, new BuiltWithSonarCause());
    
    // Redirect client to project hom page
    resp.sendRedirect(project.getAbsoluteUrl());
  }
  
  public static class BuiltWithSonarCause extends UserCause {
  
  }

}
