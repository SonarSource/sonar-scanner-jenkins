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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ComplexityThresholdsUtils {

  private static final String PARSING_SEPARATOR = ":";
  private static final String EQUALITY_SEPARATOR = "=";

  public static List<ComplexityThreshold> convertAbacusThresholdsToComplexityThresholds(String[] propertyThresholds) {

    List<ComplexityThreshold> complexityThresholds = new ArrayList<ComplexityThreshold>();
    String[] temp;

    for (String propertyThreshold : propertyThresholds) {
      temp = propertyThreshold.split(PARSING_SEPARATOR);
      Double thresholdValue = null;
      if (temp.length > 1) {
        thresholdValue = Double.valueOf(temp[1]);
      }
      complexityThresholds.add(new ComplexityThreshold(temp[0], thresholdValue));
    }

    return complexityThresholds;
  }

  public static String convertCyclomaticComplexityToAbacusComplexity(Double cyclomaticComplexity, List<ComplexityThreshold> complexityThresholds) {

    String complexity = null;

    for (ComplexityThreshold complexityThreshold : complexityThresholds) {
      if (complexityThreshold.getThreshold() == null) {
        complexity = complexityThreshold.getComplexityName();
        break;
      } else {
        if (cyclomaticComplexity.doubleValue() <= complexityThreshold.getThreshold().doubleValue()) {
          complexity = complexityThreshold.getComplexityName();
          break;
        }
      }
    }
    return complexity;
  }

  public static void initCounterThreshold(List<ComplexityThreshold> complexityThresholds) {
    for (ComplexityThreshold complexityThreshold : complexityThresholds) {
      complexityThreshold.initializeCounter();
    }
  }

  public static String buildComplexityDistributionMeasureValue(List<ComplexityThreshold> complexityThresholds) {
    String complexityDistributionMeasureValue = "";
    for (Iterator<ComplexityThreshold> it = complexityThresholds.iterator(); it.hasNext();) {
      ComplexityThreshold complexityThreshold = it.next();
      complexityDistributionMeasureValue += (complexityThreshold.getComplexityName() + EQUALITY_SEPARATOR
          + complexityThreshold.getCounter());
      if (it.hasNext()) {
        complexityDistributionMeasureValue += ";";
      }
    }
    return complexityDistributionMeasureValue;
  }

}
