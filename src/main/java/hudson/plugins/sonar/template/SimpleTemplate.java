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
package hudson.plugins.sonar.template;

import static org.apache.commons.io.IOUtils.closeQuietly;
import hudson.FilePath;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

public class SimpleTemplate {
  private String template;

  public SimpleTemplate(String path) {
    InputStream stream = null;
    try {
      stream = getClass().getClassLoader().getResourceAsStream(path);
      if (stream == null) {
        throw new TemplateException("Template not found in classloader: " + path);
      }
      template = IOUtils.toString(stream);
    }
    catch (IOException e) {
      throw new TemplateException("Could not read template: " + path, e);
    }
    finally {
      closeQuietly(stream);
    }
  }

  public void setAttribute(String key, String value) {
    template = StringUtils.replace(template, '$' + key + '$', value);
  }

  @Override
  public String toString() {
    return template;
  }

  public void write(FilePath path) throws IOException, InterruptedException {
    FilePath pom = path.child("sonar-pom.xml");
    OutputStreamWriter outputStream = new OutputStreamWriter(pom.write());
    try {
      outputStream.write(template);
    }
    finally {
      outputStream.close();
    }
  }
}
