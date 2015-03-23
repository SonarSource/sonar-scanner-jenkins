/*
 * Sonar Abacus Plugin
 * Copyright (C) 2012 David FRANCOIS and David RACODON
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.abacus;

import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.SonarPlugin;
import org.sonar.plugins.abacus.chart.BarChart3D;
import org.sonar.plugins.abacus.chart.PieChart3D;

import java.util.Arrays;
import java.util.List;

@Properties({
  @Property(key = AbacusPlugin.ABACUS_COMPLEXITY_THRESHOLDS,
    name = "Abacus complexity thresholds",
    global = true, project = true, module = false,
    defaultValue = "Simple:20, Medium:50, Complex:100, Very complex")
})
public class AbacusPlugin extends SonarPlugin {

  public static final String ABACUS_COMPLEXITY_THRESHOLDS = "sonar.abacus.complexityThresholds";

  public List getExtensions() {
    return Arrays.asList(AbacusMetrics.class, AbacusDecorator.class, AbacusWidget.class, AbacusTab.class, PieChart3D.class, BarChart3D.class);
  }

}
