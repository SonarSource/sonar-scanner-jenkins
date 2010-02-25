package hudson.plugins.sonar.utils;

import hudson.util.ArgumentListBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

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
    return Arrays.asList(new Object[][]{
        {true},
        {false},
    });
  }
  
  @Test
  public void empty() {
    builder.append("key", null);
    assertEquals("", original.toStringWithQuote());
  }

  @Test
  public void ampersand() {
    builder.append("key", "&");
    if (builder.isUnix()) {
      assertEquals("-Dkey=&", original.toStringWithQuote());
    } else {
      assertEquals("\"-Dkey=&\"" , original.toStringWithQuote());
    }
  }

  @Test
  public void withoutAmpersand() {
    builder.append("key", "value");
    assertEquals("-Dkey=value", original.toStringWithQuote());
  }
}
