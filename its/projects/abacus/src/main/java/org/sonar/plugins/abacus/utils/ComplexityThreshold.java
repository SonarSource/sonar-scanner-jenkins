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
package org.sonar.plugins.abacus.utils;

public final class ComplexityThreshold {

  private final String complexityName;

  private final Double threshold;

  private int counter;

  public ComplexityThreshold(final String complexityName, final Double threshold) {
    this.complexityName = complexityName;
    this.threshold = threshold;
    this.counter = 0;
  }

  public String getComplexityName() {
    return complexityName;
  }

  public Double getThreshold() {
    return threshold;
  }

  public int getCounter() {
    return counter;
  }

  public void initializeCounter() {
    this.counter = 0;
  }

  public void incrementCounter(final int increment) {
    this.counter += increment;
  }

}
