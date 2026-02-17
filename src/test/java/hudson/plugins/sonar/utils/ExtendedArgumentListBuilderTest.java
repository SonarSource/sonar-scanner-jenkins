/*
 * SonarQube Scanner for Jenkins
 * Copyright (C) 2007-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package hudson.plugins.sonar.utils;

import hudson.util.ArgumentListBuilder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * SONARPLUGINS-123, SONARPLUGINS-363, SONARPLUGINS-385
 *
 * @author Evgeny Mandrikov
 */
class ExtendedArgumentListBuilderTest {

  /**
   * See SONARPLUGINS-392
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void spaces(boolean unix) {
    ArgumentListBuilder original = new ArgumentListBuilder();
    ExtendedArgumentListBuilder builder = new ExtendedArgumentListBuilder(original, unix);

    builder.append("key", " value ");
    assertThat(original.toStringWithQuote(), is("-Dkey=value"));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void empty(boolean unix) {
    ArgumentListBuilder original = new ArgumentListBuilder();
    ExtendedArgumentListBuilder builder = new ExtendedArgumentListBuilder(original, unix);

    builder.append("key1", null);
    builder.append("key2", "");
    builder.appendMasked("key3", null);
    builder.appendMasked("key4", "");
    assertThat(original.toStringWithQuote(), is(""));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void ampersand(boolean unix) {
    ArgumentListBuilder original = new ArgumentListBuilder();
    ExtendedArgumentListBuilder builder = new ExtendedArgumentListBuilder(original, unix);

    builder.append("key", "&");
    if (builder.isUnix()) {
      assertThat(original.toStringWithQuote(), is("-Dkey=&"));
    } else {
      assertThat(original.toStringWithQuote(), is("\"-Dkey=&\""));
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void withoutAmpersand(boolean unix) {
    ArgumentListBuilder original = new ArgumentListBuilder();
    ExtendedArgumentListBuilder builder = new ExtendedArgumentListBuilder(original, unix);

    builder.append("key", "value");
    assertThat(original.toStringWithQuote(), is("-Dkey=value"));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void mixed(boolean unix) {
    ArgumentListBuilder original = new ArgumentListBuilder();
    ExtendedArgumentListBuilder builder = new ExtendedArgumentListBuilder(original, unix);

    builder.append("key", "value");
    builder.append("amp", "&");
    if (builder.isUnix()) {
      assertThat(original.toStringWithQuote(), is("-Dkey=value -Damp=&"));
    } else {
      assertThat(original.toStringWithQuote(), is("-Dkey=value \"-Damp=&\""));
    }
  }
}
