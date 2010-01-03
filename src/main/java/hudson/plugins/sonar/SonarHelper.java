/*
 * Sonar, entreprise quality control tool.
 * Copyright (C) 2007-2008 Hortis-GRC SA
 * mailto:be_agile HAT hortis DOT ch
 *
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

package hudson.plugins.sonar;

import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.util.ArgumentListBuilder;
import org.apache.commons.lang.StringUtils;

import java.util.List;

/**
 * @author Evgeny Mandrikov
 */
public final class SonarHelper {
  /**
   * Hide utility-class constructor.
   */
  private SonarHelper() {
  }

  public static void appendUnlessEmpty(StringBuilder builder, String key, String value) {
    if (StringUtils.isNotEmpty(StringUtils.defaultString(value))) {
      builder.append(" -D");
      builder.append(key);
      builder.append('=');
      builder.append(value.contains(" ") ? "'" + value + "'" : value);
    }
  }

  public static void addTokenizedAndQuoted(boolean isUnix, ArgumentListBuilder args, String argsString) {
    if (StringUtils.isNotBlank(argsString)) {
      for (String argToken : Util.tokenize(argsString)) {
        // see SONARPLUGINS-123 amperstand bug with windows..
        if (!isUnix && argToken.contains("&")) {
          args.addQuoted(argToken);
        } else {
          args.add(argToken);
        }
      }
    }
  }

  /**
   * Returns true, if specified build triggered by specified trigger.
   *
   * @param build   build
   * @param trigger trigger
   * @return true, if specified build triggered by specified trigger
   */
  public static boolean isTrigger(AbstractBuild<?, ?> build, Class<? extends hudson.model.Cause> trigger) {
    CauseAction buildCause = build.getAction(CauseAction.class);
    List<Cause> buildCauses = buildCause.getCauses();
    for (Cause cause : buildCauses) {
      if (cause.getClass().equals(trigger)) {
        return true;
      }
    }
    return false;
  }
}
