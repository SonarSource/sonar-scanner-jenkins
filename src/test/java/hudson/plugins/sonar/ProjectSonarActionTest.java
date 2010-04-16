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
