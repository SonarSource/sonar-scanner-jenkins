package hudson.plugins.sonar;

import hudson.plugins.sonar.utils.MagicNames;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
    assertNotNull(action.getDisplayName());
    assertNotNull(action.getIconFileName());
    assertEquals(MagicNames.DEFAULT_SONAR_URL, action.getUrlName());
  }
}
