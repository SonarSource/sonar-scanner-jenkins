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

import hudson.model.AbstractBuild;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SonarUtilsTest {

  @Test
  public void shouldParseUrlInLogs() throws Exception {
    assertThat(SonarUtils.extractSonarProjectURLFromLogs(mockedBuild(""))).isNull();
    String log = "foo\n" +
      "[INFO] [16:36:31.386] ANALYSIS SUCCESSFUL, you can browse http://sonar:9000/dashboard/index/myproject:onbranch\n"
      + "bar";
    assertThat(SonarUtils.extractSonarProjectURLFromLogs(mockedBuild(log))).isEqualTo("http://sonar:9000/dashboard/index/myproject:onbranch");
  }

  private AbstractBuild<?, ?> mockedBuild(String log) throws IOException {
    AbstractBuild<?, ?> build = mock(AbstractBuild.class);
    when(build.getLogReader()).thenReturn(new StringReader(log));
    return build;
  }

}
