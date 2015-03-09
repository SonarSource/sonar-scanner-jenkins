/*
 * Jenkins Plugin for SonarQube, open source software quality management tool.
 * mailto:contact AT sonarsource DOT com
 *
 * Jenkins Plugin for SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Jenkins Plugin for SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
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
package hudson.plugins.sonar.utils;

import hudson.util.ArgumentListBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * SONARPLUGINS-123, SONARPLUGINS-363, SONARPLUGINS-385
 *
 * @author Evgeny Mandrikov
 */
@RunWith(Parameterized.class)
public class ExtendedArgumentListBuilderTest {
  private ArgumentListBuilder original;
  private ExtendedArgumentListBuilder builder;

  public ExtendedArgumentListBuilderTest(boolean unix) {
    original = new ArgumentListBuilder();
    builder = new ExtendedArgumentListBuilder(original, unix);
  }

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
      {true},
      {false},
    });
  }

  /**
   * See SONARPLUGINS-392
   */
  @Test
  public void spaces() {
    builder.append("key", " value ");
    assertThat(original.toStringWithQuote(), is("-Dkey=value"));
  }

  @Test
  public void empty() {
    builder.append("key1", null);
    builder.append("key2", "");
    builder.appendMasked("key3", null);
    builder.appendMasked("key4", "");
    assertThat(original.toStringWithQuote(), is(""));
  }

  @Test
  public void ampersand() {
    builder.append("key", "&");
    if (builder.isUnix()) {
      assertThat(original.toStringWithQuote(), is("-Dkey=&"));
    } else {
      assertThat(original.toStringWithQuote(), is("\"-Dkey=&\""));
    }
  }

  @Test
  public void withoutAmpersand() {
    builder.append("key", "value");
    assertThat(original.toStringWithQuote(), is("-Dkey=value"));
  }

  @Test
  public void mixed() {
    builder.append("key", "value");
    builder.append("amp", "&");
    if (builder.isUnix()) {
      assertThat(original.toStringWithQuote(), is("-Dkey=value -Damp=&"));
    } else {
      assertThat(original.toStringWithQuote(), is("-Dkey=value \"-Damp=&\""));
    }
  }
}
