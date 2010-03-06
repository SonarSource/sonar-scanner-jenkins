package hudson.plugins.sonar;

import hudson.plugins.sonar.utils.MagicNames;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Evgeny Mandrikov
 */
public class ProjectSonarActionTest {
  private ProjectSonarAction action;

  @Before
  public void setUp() throws Exception {
    action = new ProjectSonarAction(MagicNames.DEFAULT_SONAR_URL);
  }

  @Test
  public void test() throws Exception {
    assertThat(action.getDisplayName(), notNullValue());
    assertThat(action.getIconFileName(), notNullValue());
    assertThat(action.getUrlName(), is(MagicNames.DEFAULT_SONAR_URL));
  }
}
