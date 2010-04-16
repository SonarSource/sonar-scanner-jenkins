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
