package hudson.plugins.sonar;

import hudson.plugins.sonar.utils.MagicNames;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Evgeny Mandrikov
 */
public class SonarInstallationTest {
  @Test
  public void defaultUrl() throws Exception {
    SonarInstallation installation = create(null);
    assertEquals(MagicNames.DEFAULT_SONAR_URL, installation.getServerLink());

    installation = create(" ");
    assertEquals(MagicNames.DEFAULT_SONAR_URL, installation.getServerLink());
  }

  @Test
  public void publicUrl() {
    SonarInstallation installation = create("http://localhost");
    installation.setServerPublicUrl("http://host:port/sonar");
    assertEquals("http://host:port/sonar", installation.getServerLink());
  }

  @Test
  public void trailingSlashAndSpace() throws Exception {
    SonarInstallation installation = create("http://host:port/sonar/ ");
    assertEquals("http://host:port/sonar", installation.getServerLink());
  }

  private SonarInstallation create(String url) {
    return new SonarInstallation("default", false, url, null, "", "", "", "", "", null);
  }
}
