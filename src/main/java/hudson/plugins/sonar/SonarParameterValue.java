/**
 * 
 */
package hudson.plugins.sonar;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.model.BooleanParameterValue;

/**
 * @author domi
 *
 */
public class SonarParameterValue extends BooleanParameterValue {

	@DataBoundConstructor
	public SonarParameterValue(String name, boolean value) {
		super(name, value);
	}

	public SonarParameterValue(String name, boolean value, String description) {
		super(name, value, description);
	}

}
