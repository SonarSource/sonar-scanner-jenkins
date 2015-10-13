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
package hudson.plugins.sonar.utils;

import hudson.console.LineTransformationOutputStream;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Class taken from the mask-passwords plugin
 */
public class MaskPasswordsOutputStream extends LineTransformationOutputStream {
  private final static String REPLACEMENT = "******";
  private final OutputStream logger;
  private final Pattern passwordsAsPattern;

  public MaskPasswordsOutputStream(OutputStream logger, Collection<String> passwords) {

    this.logger = logger;

    if (passwords != null && passwords.size() > 0) {
      // passwords are aggregated into a regex which is compiled as a pattern
      // for efficiency
      StringBuilder regex = new StringBuilder().append('(');

      int nbMaskedPasswords = 0;
      for (String password : passwords) {
        if (StringUtils.isNotEmpty(password)) { // we must not handle empty passwords
          regex.append(Pattern.quote(password));
          regex.append('|');
          nbMaskedPasswords++;
        }
      }
      if (nbMaskedPasswords >= 1) { // is there at least one password to mask?
        regex.deleteCharAt(regex.length() - 1); // removes the last unuseful pipe
        regex.append(')');
        passwordsAsPattern = Pattern.compile(regex.toString());
      } else { // no passwords to hide
        passwordsAsPattern = null;
      }
    } else { // no passwords to hide
      passwordsAsPattern = null;
    }
  }

  @Override
  protected void eol(byte[] bytes, int len) throws IOException {
    String line = new String(bytes, 0, len);
    if (passwordsAsPattern != null) {
      line = passwordsAsPattern.matcher(line).replaceAll(REPLACEMENT);
    }
    logger.write(line.getBytes());
  }

  @Override
  public void close() throws IOException {
    super.close();
    logger.close();
  }
}
