package hudson.plugins.sonar.model;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Evgeny Mandrikov
 */
public class ReportsConfig {
  /**
   * Optional.
   */
  private final String surefireReportsPath;

  /**
   * Optional.
   */
  private final String coberturaReportPath;

  /**
   * Optional.
   */
  private final String cloverReportPath;

  public ReportsConfig() {
    this(null, null, null);
  }

  @DataBoundConstructor
  public ReportsConfig(String surefireReportsPath, String coberturaReportPath, String cloverReportPath) {
    this.surefireReportsPath = surefireReportsPath;
    this.coberturaReportPath = coberturaReportPath;
    this.cloverReportPath = cloverReportPath;
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
