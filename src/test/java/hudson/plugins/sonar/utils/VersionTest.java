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
package hudson.plugins.sonar.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class VersionTest {
  @Test
  public void test1() {
    Version a = new Version("1.1");
    Version b = new Version("1.1.1");
    assertThat(a.compareTo(b)).isLessThan(0); // return -1 (a<b)
    assertThat(a.equals(b)).isFalse(); // return false
    assertThat(a.hashCode()).isNotEqualTo(b.hashCode());
  }

  @Test
  public void test2() {
    Version a = new Version("2.0");
    Version b = new Version("1.9.9");
    assertThat(a.compareTo(b)).isGreaterThan(0); // return 1 (a>b)
    assertThat(a.equals(b)).isFalse(); // return false
    assertThat(a.hashCode()).isNotEqualTo(b.hashCode());
  }

  @Test
  public void testSnapshot() {
    new Version("2.0-SNAPSHOT");
  }

  @Test
  public void testOnlyMajor() {
    Version a = new Version("1.0.0");
    Version b = new Version("1.0");
    assertThat(a.compareTo(b)).isZero(); // return 0 (a=b)
    assertThat(a.equals(b)).isTrue(); // return true
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
  }

  @Test
  public void testToString() {
    Version v = new Version("1.2.3");
    assertThat(v.toString()).isEqualTo("1.2.3");
  }

  @Test
  public void testSort() {
    List<Version> versions = new ArrayList<Version>();
    versions.add(new Version("2.0"));
    versions.add(new Version("1.0.5"));
    versions.add(new Version("1.01.0"));
    versions.add(new Version("1.00.1"));
    assertThat(Collections.min(versions).get()).isEqualTo("1.00.1"); // return min version
    assertThat(Collections.max(versions).get()).isEqualTo("2.0"); // return max version

  }

  @Test
  public void testDontRound() {
    Version a = new Version("2.06");
    Version b = new Version("2.060");
    assertThat(a.equals(b)).isFalse(); // return false
    assertThat(a.hashCode()).isNotEqualTo(b.hashCode());
  }
}
