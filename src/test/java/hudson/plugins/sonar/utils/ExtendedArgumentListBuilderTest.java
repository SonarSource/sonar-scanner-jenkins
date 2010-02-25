package hudson.plugins.sonar.utils;

import hudson.util.ArgumentListBuilder;
import junit.framework.TestCase;

/**
 * SONARPLUGINS-123, SONARPLUGINS-363, SONARPLUGINS-385
 *
 * @author Evgeny Mandrikov
 */
public class ExtendedArgumentListBuilderTest extends TestCase {

  private static final String URL = "mysql://host:port/sonar?useUnicode=true&characterEncoding=utf8";
  private static final String URL2 = "mysql://host:port/sonar?useUnicode=true";

  public void testAmpersandOnUnix() throws Exception {
    ArgumentListBuilder original = new ArgumentListBuilder();
    ExtendedArgumentListBuilder builder = new ExtendedArgumentListBuilder(original, true);
    builder.append("sonar.jdbc.url", URL);
    assertEquals("-Dsonar.jdbc.url=" + URL, original.toStringWithQuote());
  }

  public void testAmpersandOnWindows() throws Exception {
    ArgumentListBuilder original = new ArgumentListBuilder();
    ExtendedArgumentListBuilder builder = new ExtendedArgumentListBuilder(original, false);
    builder.append("sonar.jdbc.url", URL);
    assertEquals("\"-Dsonar.jdbc.url=" + URL + "\"", original.toStringWithQuote());
  }

  public void testWithoutAmpersandOnUnix() {
    ArgumentListBuilder original = new ArgumentListBuilder();
    ExtendedArgumentListBuilder builder = new ExtendedArgumentListBuilder(original, true);
    builder.append("sonar.jdbc.url", URL2);
    assertEquals(
        "-Dsonar.jdbc.url=" + URL2,
        original.toStringWithQuote()
    );
  }

  public void testWithoutAmpersandOnWindows() {
    ArgumentListBuilder original = new ArgumentListBuilder();
    ExtendedArgumentListBuilder builder = new ExtendedArgumentListBuilder(original, false);
    builder.append("sonar.jdbc.url", URL2);
    assertEquals(
        "-Dsonar.jdbc.url=" + URL2,
        original.toStringWithQuote()
    );
  }
}
