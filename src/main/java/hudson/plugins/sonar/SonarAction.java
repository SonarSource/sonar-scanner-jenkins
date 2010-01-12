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
package hudson.plugins.sonar;

import hudson.model.ProminentProjectAction;
import hudson.plugins.sonar.utils.MagicNames;

/**
 * @author Evgeny Mandrikov
 * @since 1.2
 */
public abstract class SonarAction implements ProminentProjectAction {
  /**
   * Default no-arg constructor.
   */
  public SonarAction() {
  }

  public String getIconFileName() {
    return MagicNames.ICON;
  }

  public String getDisplayName() {
    return Messages.SonarAction_Sonar();
  }

  public String getUrlName() {
    return null;
  }
}
