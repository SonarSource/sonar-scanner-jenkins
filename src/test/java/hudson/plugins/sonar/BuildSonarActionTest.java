package hudson.plugins.sonar;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Evgeny Mandrikov
 */
public class BuildSonarActionTest {
  private BuildSonarAction action;

  @Before
  public void setUp() {
    action = new BuildSonarAction();
  }

  @Test
  public void test() throws Exception {
    assertNull(action.getIconFileName());
    assertNull(action.getUrlName());

    assertNotNull(action.getDisplayName());
    assertNotNull(action.getIcon());
    assertNotNull(action.getTooltip());
  }
}
