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
    return Arrays.asList(new Object[][]{
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
