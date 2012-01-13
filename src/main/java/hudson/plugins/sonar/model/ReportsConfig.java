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
package hudson.plugins.sonar.model;

/**
 * @since 1.2
 * @deprecated in 1.7
 */
@Deprecated
public class ReportsConfig {
  /**
   * Optional.
   */
  private String surefireReportsPath;

  /**
   * Optional.
   */
  private String coberturaReportPath;

  /**
   * Optional.
   */
  private String cloverReportPath;

  private ReportsConfig() {
  }

  public String getSurefireReportsPath() {
    return surefireReportsPath;
  }

  public String getCoberturaReportPath() {
    return coberturaReportPath;
  }

  public String getCloverReportPath() {
    return cloverReportPath;
  }
}
