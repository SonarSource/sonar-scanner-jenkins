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
package org.sonar.plugins.abacus.chart;

import org.apache.commons.lang.StringUtils;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.chart.plot.Plot;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.ui.RectangleInsets;
import org.jfree.util.Rotation;
import org.sonar.api.charts.AbstractChart;
import org.sonar.api.charts.ChartParameters;

import java.awt.Color;
import java.awt.Font;

public class PieChart3D extends AbstractChart {

  public static final Font DEFAULT_FONT = new Font("SansSerif", Font.PLAIN, 12);

  public static final String DEFAULT_MESSAGE_NODATA = "No data available";
  public static final String PARAM_VALUES = "v";
  public static final String PARAM_COLORS = "c";

  public String getKey() {
    return "pieChart3D";
  }

  @Override
  protected Plot getPlot(ChartParameters params) {
    PiePlot3D plot = new PiePlot3D();

    String[] colorsHex = params.getValues(PARAM_COLORS, ",", true);
    String[] serie = params.getValues(PARAM_VALUES, ";", true);

    DefaultPieDataset set = new DefaultPieDataset();

    Color[] colors = COLORS;
    if (colorsHex != null && colorsHex.length > 0) {
      colors = new Color[colorsHex.length];
      for (int i = 0; i < colorsHex.length; i++) {
        colors[i] = Color.decode("#" + colorsHex[i]);
      }
    }

    String[] keyValue = null;
    for (int i = 0; i < serie.length; i++) {
      if (!StringUtils.isEmpty(serie[i])) {
        keyValue = StringUtils.split(serie[i], "=");
        set.setValue(keyValue[0], Double.parseDouble(keyValue[1]));
        plot.setSectionPaint(keyValue[0], colors[i]);
      }
    }
    plot.setDataset(set);

    plot.setStartAngle(360);
    plot.setCircular(true);
    plot.setDirection(Rotation.CLOCKWISE);
    plot.setNoDataMessage(DEFAULT_MESSAGE_NODATA);
    plot.setInsets(RectangleInsets.ZERO_INSETS);
    plot.setForegroundAlpha(1.0f);
    plot.setBackgroundAlpha(0.0f);
    plot.setIgnoreNullValues(true);
    plot.setIgnoreZeroValues(true);
    plot.setOutlinePaint(Color.WHITE);
    plot.setShadowPaint(Color.WHITE);
    plot.setDarkerSides(false);
    plot.setLabelFont(DEFAULT_FONT);
    plot.setLabelPaint(Color.BLACK);
    plot.setLabelBackgroundPaint(Color.WHITE);
    plot.setLabelOutlinePaint(Color.WHITE);
    plot.setLabelShadowPaint(Color.WHITE);
    plot.setLabelPadding(new RectangleInsets(1, 1, 1, 1));
    plot.setInteriorGap(0.02);
    plot.setMaximumLabelWidth(0.15);

    return plot;
  }

}
