package hudson.plugins.sonar;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;


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
    assertThat(action.getIconFileName(), nullValue());
    assertThat(action.getUrlName(), nullValue());

    assertThat(action.getDisplayName(), notNullValue());
    assertThat(action.getIcon(), notNullValue());
    assertThat(action.getTooltip(), notNullValue());
  }
}
