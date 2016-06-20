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

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MaskPasswordsOutputStreamTest {
  private ByteArrayOutputStream os;

  @Test
  public void test() throws IOException {
    BufferedWriter w = getWriter("pass1", "admin");
    w.write("password=pass1");
    w.newLine();
    w.write("password=pass2");
    w.newLine();
    w.write("our admin is");
    w.newLine();
    w.write("nothing to hide");
    // flush
    w.close();

    assertWritten("password=******", "password=pass2", "our ****** is", "nothing to hide");
  }

  @Test
  public void dontMaskUrl() throws IOException {
    String t = "ANALYSIS SUCCESSFUL, you can browse http://localhost:9000/sonar";
    BufferedWriter w = getWriter("sonar");
    w.write(t);
    w.newLine();
    w.write("password=sonar");
    w.newLine();
    w.close();

    assertWritten(t, "password=******");
  }

  private BufferedWriter getWriter(String... passwords) {
    os = new ByteArrayOutputStream();
    MaskPasswordsOutputStream filteredOs = new MaskPasswordsOutputStream(os, StandardCharsets.UTF_8, Arrays.asList(passwords));
    return new BufferedWriter(new OutputStreamWriter(filteredOs));
  }

  private void assertWritten(String... str) {
    String written = new String(os.toByteArray(), StandardCharsets.UTF_8);
    String[] lines = written.split(System.lineSeparator());

    assertThat(lines).containsExactly(str);
  }
}
