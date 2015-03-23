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

import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;

import java.util.Arrays;
import java.util.List;

public final class AbacusMetrics implements Metrics {

  private static final String ABACUS_DOMAIN = "Abacus";

  public static final Metric ABACUS_COMPLEXITY = new Metric.Builder("abacus-complexity", "Abacus complexity", Metric.ValueType.STRING)
      .setDescription("Abacus complexity")
      .setQualitative(false)
      .setDomain(ABACUS_DOMAIN)
      .create();

  public static final Metric ABACUS_COMPLEXITY_DISTRIBUTION = new Metric.Builder("abacus-complexity-distribution", "Abacus complexity distribution", Metric.ValueType.DISTRIB)
      .setDescription("Abacus complexity distribution")
      .setQualitative(false)
      .setDomain(ABACUS_DOMAIN)
      .create();

  public List<Metric> getMetrics() {
    return Arrays.asList(ABACUS_COMPLEXITY, ABACUS_COMPLEXITY_DISTRIBUTION);
  }

}
