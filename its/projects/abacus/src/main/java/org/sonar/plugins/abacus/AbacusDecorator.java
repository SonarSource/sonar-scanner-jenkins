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

import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasureUtils;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.plugins.abacus.utils.ComplexityThreshold;
import org.sonar.plugins.abacus.utils.ComplexityThresholdsUtils;

import java.util.Arrays;
import java.util.List;

public final class AbacusDecorator implements Decorator {

  private List<ComplexityThreshold> complexityThresholds;
  private Settings settings;

  public AbacusDecorator(Settings settings) {
    this.settings = settings;
  }

  @DependsUpon
  public List<Metric> dependsOn() {
    return Arrays.asList(CoreMetrics.FILE_COMPLEXITY);
  }

  @DependedUpon
  public List<Metric> dependedOn() {
    return Arrays.asList(AbacusMetrics.ABACUS_COMPLEXITY, AbacusMetrics.ABACUS_COMPLEXITY_DISTRIBUTION);
  }

  public boolean shouldExecuteOnProject(Project project) {
    initAbacus();
    return true;
  }

  public void decorate(Resource rsrc, DecoratorContext dc) {
    computeAbacusComplexity(rsrc, dc);
    computeAbacusComplexityDistribution(rsrc, dc);
  }

  private void initAbacus() {
    String[] abacusList = settings.getStringArray(AbacusPlugin.ABACUS_COMPLEXITY_THRESHOLDS);
    complexityThresholds = ComplexityThresholdsUtils.convertAbacusThresholdsToComplexityThresholds(abacusList);
  }

  private void computeAbacusComplexity(Resource rsrc, DecoratorContext dc) {
    if (ResourceUtils.isFile(rsrc) || ResourceUtils.isPackage(rsrc) || ResourceUtils.isDirectory(rsrc)
      || ResourceUtils.isRootProject(rsrc) || ResourceUtils.isModuleProject(rsrc)) {
      Double fileComplexity = MeasureUtils.getValue(dc.getMeasure(CoreMetrics.FILE_COMPLEXITY), Double.NaN);
      if (!Double.isNaN(fileComplexity)) {
        dc.saveMeasure(new Measure(AbacusMetrics.ABACUS_COMPLEXITY, ComplexityThresholdsUtils.convertCyclomaticComplexityToAbacusComplexity(fileComplexity, complexityThresholds)));
      }
    }
  }

  private void computeAbacusComplexityDistribution(Resource rsrc, DecoratorContext dc) {

    if (ResourceUtils.isPackage(rsrc) || ResourceUtils.isDirectory(rsrc)) {
      ComplexityThresholdsUtils.initCounterThreshold(complexityThresholds);
      for (Measure measure : dc.getChildrenMeasures(AbacusMetrics.ABACUS_COMPLEXITY)) {
        for (ComplexityThreshold complexityThreshold : complexityThresholds) {
          if (measure.getData() != null && measure.getData().equals(complexityThreshold.getComplexityName())) {
            complexityThreshold.incrementCounter(1);
            break;
          }
        }
      }
      dc.saveMeasure(new Measure(AbacusMetrics.ABACUS_COMPLEXITY_DISTRIBUTION, ComplexityThresholdsUtils.buildComplexityDistributionMeasureValue(complexityThresholds)));
    } else if (ResourceUtils.isRootProject(rsrc) || ResourceUtils.isModuleProject(rsrc)) {
      ComplexityThresholdsUtils.initCounterThreshold(complexityThresholds);
      for (Measure measure : dc.getChildrenMeasures(AbacusMetrics.ABACUS_COMPLEXITY_DISTRIBUTION)) {
        String[] distribution = measure.getData().split(";");
        for (String aDistribution : distribution) {
          String[] tmp = aDistribution.split("=");
          for (ComplexityThreshold complexityThreshold : complexityThresholds) {
            if (tmp[0].equals(complexityThreshold.getComplexityName())) {
              complexityThreshold.incrementCounter(Integer.parseInt(tmp[1]));
              break;
            }
          }
        }
      }
      dc.saveMeasure(new Measure(AbacusMetrics.ABACUS_COMPLEXITY_DISTRIBUTION, ComplexityThresholdsUtils.buildComplexityDistributionMeasureValue(complexityThresholds)));
    }

  }

}
