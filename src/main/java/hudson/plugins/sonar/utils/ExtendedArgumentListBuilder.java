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

  /**
   * Appends specified key/value pair, if value not empty.
   * Also value will be trimmed (see <a href="http://jira.codehaus.org/browse/MNG-3529">MNG-3529</a>).
   *
   * @param key   key
   * @param value value
   * @see #append(String)
   */
  public void append(String key, String value) {
    value = StringUtils.trimToEmpty(value);
    if (StringUtils.isNotEmpty(value)) {
      append("-D" + key + "=" + value);
    }
  }

  /**
   * Appends specified key/value pair with mask, if value not empty.
   * Also value will be trimmed (see <a href="http://jira.codehaus.org/browse/MNG-3529">MNG-3529</a>).
   *
   * @param key   key
   * @param value value
   * @see hudson.util.ArgumentListBuilder#addMasked(String)
   */
  public void appendMasked(String key, String value) {
    value = StringUtils.trimToEmpty(value);
    if (StringUtils.isNotEmpty(value)) {
      builder.addMasked("-D" + key + "=" + value);
    }
  }

  /**
   * Appends specified argument with proper quoting under unix and other OSes.
   *
   * @param arg argument
   * @see hudson.util.ArgumentListBuilder#add(String)
   * @see hudson.util.ArgumentListBuilder#addQuoted(String)
   */
  public void append(String arg) {
    if (!isUnix() && arg.contains("&")) {
      builder.addQuoted(arg);
    } else {
      builder.add(arg);
    }
  }

  public boolean isUnix() {
    return unix;
  }
}
