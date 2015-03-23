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

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ComplexityThresholdsUtilsTest {

  @Test
  public void testConvertAbacusThresholdsToComplexityThresholds() {

    List<ComplexityThreshold> complexityThresholds;

    complexityThresholds = ComplexityThresholdsUtils.convertAbacusThresholdsToComplexityThresholds(new String[] {"Simple"});
    Assert.assertNotNull(complexityThresholds);
    Assert.assertEquals(complexityThresholds.size(), 1);
    Assert.assertEquals(complexityThresholds.get(0).getComplexityName(), "Simple");
    Assert.assertEquals(complexityThresholds.get(0).getThreshold(), null);

    complexityThresholds = ComplexityThresholdsUtils.convertAbacusThresholdsToComplexityThresholds(new String[] {"Simple:10", "Complex"});
    Assert.assertNotNull(complexityThresholds);
    Assert.assertEquals(complexityThresholds.size(), 2);
    Assert.assertEquals(complexityThresholds.get(0).getComplexityName(), "Simple");
    Assert.assertEquals(complexityThresholds.get(0).getThreshold(), Double.valueOf(10.0));
    Assert.assertEquals(complexityThresholds.get(1).getComplexityName(), "Complex");
    Assert.assertEquals(complexityThresholds.get(1).getThreshold(), null);

  }

  @Test
  public void testConvertCyclomaticComplexityToAbacusComplexity() {

    List<ComplexityThreshold> complexityThresholds = new ArrayList<ComplexityThreshold>();
    complexityThresholds.add(new ComplexityThreshold("Simple", 10.0));
    complexityThresholds.add(new ComplexityThreshold("Medium", 30.0));
    complexityThresholds.add(new ComplexityThreshold("Complex", 60.0));
    complexityThresholds.add(new ComplexityThreshold("Very complex", null));

    Assert.assertEquals("Simple", ComplexityThresholdsUtils.convertCyclomaticComplexityToAbacusComplexity(5.0, complexityThresholds));
    Assert.assertEquals("Medium", ComplexityThresholdsUtils.convertCyclomaticComplexityToAbacusComplexity(23.0, complexityThresholds));
    Assert.assertEquals("Complex", ComplexityThresholdsUtils.convertCyclomaticComplexityToAbacusComplexity(36.0, complexityThresholds));
    Assert.assertEquals("Very complex", ComplexityThresholdsUtils.convertCyclomaticComplexityToAbacusComplexity(77.0, complexityThresholds));

  }

  @Test
  public void testInitCounterThreshold() {

    List<ComplexityThreshold> complexityThresholds = new ArrayList<ComplexityThreshold>();
    complexityThresholds.add(new ComplexityThreshold("Simple", 10.0));
    complexityThresholds.add(new ComplexityThreshold("Medium", 30.0));
    complexityThresholds.add(new ComplexityThreshold("Complex", 60.0));
    complexityThresholds.add(new ComplexityThreshold("Very complex", null));

    ComplexityThresholdsUtils.initCounterThreshold(complexityThresholds);

    Assert.assertEquals(0, complexityThresholds.get(0).getCounter());
    Assert.assertEquals(0, complexityThresholds.get(1).getCounter());
    Assert.assertEquals(0, complexityThresholds.get(2).getCounter());
    Assert.assertEquals(0, complexityThresholds.get(3).getCounter());

  }

  @Test
  public void testBuildComplexityDistributionMeasureValue() {

    ComplexityThreshold complexityThreshold;
    List<ComplexityThreshold> complexityThresholds = new ArrayList<ComplexityThreshold>();

    complexityThreshold = new ComplexityThreshold("Simple", 10.0);
    complexityThreshold.incrementCounter(1);
    complexityThresholds.add(complexityThreshold);

    complexityThreshold = new ComplexityThreshold("Medium", 30.0);
    complexityThreshold.incrementCounter(2);
    complexityThresholds.add(complexityThreshold);

    complexityThreshold = new ComplexityThreshold("Complex", 60.0);
    complexityThreshold.incrementCounter(3);
    complexityThresholds.add(complexityThreshold);

    complexityThreshold = new ComplexityThreshold("Very complex", null);
    complexityThreshold.incrementCounter(4);
    complexityThresholds.add(complexityThreshold);

    Assert.assertEquals("Simple=1;Medium=2;Complex=3;Very complex=4", ComplexityThresholdsUtils.buildComplexityDistributionMeasureValue(complexityThresholds));

  }
}
