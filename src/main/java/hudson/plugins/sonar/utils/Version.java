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

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Version implements Comparable<Version> {
  private String versionStr;
  private int[] parts;

  public Version(String version) {
    Objects.requireNonNull(version, "Version can not be null");
    if (!version.matches("[0-9]+(\\.[0-9]+)+(.*)?")) {
      throw new IllegalArgumentException("Invalid version format: " + version);
    }
    this.versionStr = version;
    parse();
  }

  private void parse() {
    Pattern p = Pattern.compile("^[0-9]+(?:\\.[0-9]+)+");
    Matcher m = p.matcher(versionStr);

    if (!m.find()) {
      throw new IllegalArgumentException("Failed to parse version: " + versionStr);
    }

    String numbers = m.group(0);
    String[] partsStr = numbers.split("\\.");
    parts = new int[partsStr.length];

    for (int i = 0; i < partsStr.length; i++) {
      parts[i] = Integer.parseInt(partsStr[i]);
    }
  }

  public final String get() {
    return this.versionStr;
  }

  @Override
  public String toString() {
    return this.versionStr;
  }

  @Override
  public int compareTo(Version that) {
    int[] thisParts = parts;
    int[] thatParts = that.parts;
    int length = Math.max(thisParts.length, thatParts.length);
    for (int i = 0; i < length; i++) {
      int thisPart = i < thisParts.length ? thisParts[i] : 0;
      int thatPart = i < thatParts.length ? thatParts[i] : 0;
      if (thisPart < thatPart) {
        return -1;
      }
      if (thisPart > thatPart) {
        return 1;
      }
    }
    return 0;
  }

  @Override
  public int hashCode() {
    int hash = 17;
    boolean nonZero = false;

    for (int i = parts.length - 1; i >= 0; i--) {
      int part = parts[i];
      if (!nonZero && part != 0) {
        nonZero = true;
      }
      if (nonZero) {
        hash = hash * 31 + part;
      }
    }
    return hash;
  }

  @Override
  public boolean equals(Object that) {
    if (this == that) {
      return true;
    }
    if (that == null) {
      return false;
    }
    if (this.getClass() != that.getClass()) {
      return false;
    }
    return this.compareTo((Version) that) == 0;
  }
}
