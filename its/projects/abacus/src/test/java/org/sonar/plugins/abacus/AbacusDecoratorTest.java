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

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.test.IsMeasure;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AbacusDecoratorTest {

  private final Project myProject = new Project("myProject");
  private final DecoratorContext dc = mock(DecoratorContext.class);
  private AbacusDecorator abacusDecorator;

  @Before
  public void initAbacusComplexityThresholds() {
    Settings settings = new Settings(new PropertyDefinitions(AbacusPlugin.class));
    abacusDecorator = new AbacusDecorator(settings);
    abacusDecorator.shouldExecuteOnProject(myProject);
  }

  @Test
  public void testAbacusComplexityOnDecorateFile() {

    when(dc.getMeasure(CoreMetrics.FILE_COMPLEXITY)).thenReturn(new Measure(CoreMetrics.FILE_COMPLEXITY, 5.0));
    abacusDecorator.decorate(new File("simpleFile"), dc);
    verify(dc).saveMeasure(argThat(new IsMeasure(AbacusMetrics.ABACUS_COMPLEXITY, "Simple")));

    when(dc.getMeasure(CoreMetrics.FILE_COMPLEXITY)).thenReturn(new Measure(CoreMetrics.FILE_COMPLEXITY, 50.0));
    abacusDecorator.decorate(new File("simpleFile"), dc);
    verify(dc).saveMeasure(argThat(new IsMeasure(AbacusMetrics.ABACUS_COMPLEXITY, "Medium")));

    when(dc.getMeasure(CoreMetrics.FILE_COMPLEXITY)).thenReturn(new Measure(CoreMetrics.FILE_COMPLEXITY, 50.5));
    abacusDecorator.decorate(new File("simpleFile"), dc);
    verify(dc).saveMeasure(argThat(new IsMeasure(AbacusMetrics.ABACUS_COMPLEXITY, "Complex")));

    when(dc.getMeasure(CoreMetrics.FILE_COMPLEXITY)).thenReturn(new Measure(CoreMetrics.FILE_COMPLEXITY, 1000.0));
    abacusDecorator.decorate(new File("simpleFile"), dc);
    verify(dc).saveMeasure(argThat(new IsMeasure(AbacusMetrics.ABACUS_COMPLEXITY, "Very complex")));

  }

  @Test
  public void testAbacusComplexityOnDecorateProject() {

    when(dc.getMeasure(CoreMetrics.FILE_COMPLEXITY)).thenReturn(new Measure(CoreMetrics.FILE_COMPLEXITY, 5.0));
    abacusDecorator.decorate(myProject, dc);
    verify(dc).saveMeasure(argThat(new IsMeasure(AbacusMetrics.ABACUS_COMPLEXITY, "Simple")));

    when(dc.getMeasure(CoreMetrics.FILE_COMPLEXITY)).thenReturn(new Measure(CoreMetrics.FILE_COMPLEXITY, 50.0));
    abacusDecorator.decorate(myProject, dc);
    verify(dc).saveMeasure(argThat(new IsMeasure(AbacusMetrics.ABACUS_COMPLEXITY, "Medium")));

    when(dc.getMeasure(CoreMetrics.FILE_COMPLEXITY)).thenReturn(new Measure(CoreMetrics.FILE_COMPLEXITY, 50.5));
    abacusDecorator.decorate(myProject, dc);
    verify(dc).saveMeasure(argThat(new IsMeasure(AbacusMetrics.ABACUS_COMPLEXITY, "Complex")));

    when(dc.getMeasure(CoreMetrics.FILE_COMPLEXITY)).thenReturn(new Measure(CoreMetrics.FILE_COMPLEXITY, 1000.0));
    abacusDecorator.decorate(myProject, dc);
    verify(dc).saveMeasure(argThat(new IsMeasure(AbacusMetrics.ABACUS_COMPLEXITY, "Very complex")));

  }

  @Test
  public void testAbacusComplexityDistributionOnDecorateDirectory() {

    List<Measure> childrenMeasures = Arrays.asList(
        new Measure(AbacusMetrics.ABACUS_COMPLEXITY, "Simple"),
        new Measure(AbacusMetrics.ABACUS_COMPLEXITY, "Simple"),
        new Measure(AbacusMetrics.ABACUS_COMPLEXITY, "Complex"),
        new Measure(AbacusMetrics.ABACUS_COMPLEXITY, "Complex"),
        new Measure(AbacusMetrics.ABACUS_COMPLEXITY, "Complex"),
        new Measure(AbacusMetrics.ABACUS_COMPLEXITY, "Complex"),
        new Measure(AbacusMetrics.ABACUS_COMPLEXITY, "Very complex"));

    when(dc.getChildrenMeasures(AbacusMetrics.ABACUS_COMPLEXITY)).thenReturn(childrenMeasures);
    abacusDecorator.decorate(new Directory("myDirectory"), dc);
    verify(dc).saveMeasure(argThat(new IsMeasure(AbacusMetrics.ABACUS_COMPLEXITY_DISTRIBUTION, "Simple=2;Medium=0;Complex=4;Very complex=1")));

  }

  @Test
  public void testAbacusComplexityDistributionOnDecorateProject() {

    List<Measure> childrenMeasures = Arrays.asList(
        new Measure(AbacusMetrics.ABACUS_COMPLEXITY_DISTRIBUTION, "Simple=2;Medium=0;Complex=4;Very complex=1"),
        new Measure(AbacusMetrics.ABACUS_COMPLEXITY_DISTRIBUTION, "Simple=1;Medium=1;Complex=2;Very complex=6"));

    when(dc.getChildrenMeasures(AbacusMetrics.ABACUS_COMPLEXITY_DISTRIBUTION)).thenReturn(childrenMeasures);
    abacusDecorator.decorate(myProject, dc);
    verify(dc).saveMeasure(argThat(new IsMeasure(AbacusMetrics.ABACUS_COMPLEXITY_DISTRIBUTION, "Simple=3;Medium=1;Complex=6;Very complex=7")));

  }

}
