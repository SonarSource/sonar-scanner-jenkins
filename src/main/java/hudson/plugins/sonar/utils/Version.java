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

public class Version implements Comparable<Version> {

  private String version;

  public Version(String version) {
    if (version == null)
      throw new IllegalArgumentException("Version can not be null");
    if (!version.matches("[0-9]+(\\.[0-9]+)*"))
      throw new IllegalArgumentException("Invalid version format");
    this.version = version;
  }

  public final String get() {
    return this.version;
  }

  @Override
  public int compareTo(Version that) {
    if (that == null)
      return 1;
    String[] thisParts = this.get().split("\\.");
    String[] thatParts = that.get().split("\\.");
    int length = Math.max(thisParts.length, thatParts.length);
    for (int i = 0; i < length; i++) {
      int thisPart = i < thisParts.length ? Integer.parseInt(thisParts[i]) : 0;
      int thatPart = i < thatParts.length ? Integer.parseInt(thatParts[i]) : 0;
      if (thisPart < thatPart)
        return -1;
      if (thisPart > thatPart)
        return 1;
    }
    return 0;
  }

  @Override
  public boolean equals(Object that) {
    if (this == that)
      return true;
    if (that == null)
      return false;
    if (this.getClass() != that.getClass())
      return false;
    return this.compareTo((Version) that) == 0;
  }
}
