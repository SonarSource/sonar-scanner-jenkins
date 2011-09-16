/**
 * 
 */
package hudson.plugins.sonar;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.BooleanParameterDefinition;

/**
 * @author domi
 *
 */
public class SonarParameterDefinition extends BooleanParameterDefinition {

	public static final String ENV_SKIP_SONAR = "SKIP_SONAR";
	
	/**
	 * @param name gets ignored
	 * @param defaultValue the default value
	 * @param description
	 */
	@DataBoundConstructor
	public SonarParameterDefinition(String name, boolean defaultValue, String description) {
		super(ENV_SKIP_SONAR, defaultValue, description);
	}
	
	@Extension
	public static class DescriptorImpl extends ParameterDescriptor {
		@Override
		public String getDisplayName() {
			return "Sonar";
		}

		@Override
		public String getHelpFile() {
			return "/plugin/sonar/param-sonar.html";
		}
	}

}
