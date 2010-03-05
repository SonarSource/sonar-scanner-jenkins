package hudson.plugins.sonar.utils;

import hudson.util.ArgumentListBuilder;
import org.apache.commons.lang.StringUtils;

/**
 * @author Evgeny Mandrikov
 * @since 1.3.1
 */
public class ExtendedArgumentListBuilder {
  private boolean unix;
  private ArgumentListBuilder builder;

  public ExtendedArgumentListBuilder(ArgumentListBuilder builder, boolean unix) {
    this.builder = builder;
    this.unix = unix;
  }

  public void append(String key, String value) {
    value = StringUtils.trimToEmpty(value);
    if (StringUtils.isNotEmpty(value)) {
      append("-D" + key + "=" + value);
    }
  }

  public void appendMasked(String key, String value) {
    value = StringUtils.trimToEmpty(value);
    if (StringUtils.isNotEmpty(value)) {
      builder.addMasked("-D" + key + "=" + value);
    }
  }

  public void append(String arg) {
    if (!unix && arg.contains("&")) {
      builder.addQuoted(arg);
    } else {
      builder.add(arg);
    }
  }

  public boolean isUnix() {
    return unix;
  }
}
