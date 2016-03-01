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

/**
 * Uses plain Strings, serializing specific reference versions as values to make serialization and posterior modifications easy. 
 */
public class SQServerVersions {
  public static final String SQ_5_1_OR_LOWER = "5.1";
  public static final String SQ_5_2 = "5.2";
  public static final String SQ_5_3_OR_HIGHER = "5.3";

  private SQServerVersions() {

  }
}
