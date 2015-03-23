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

import org.sonar.api.web.AbstractRubyTemplate;
import org.sonar.api.web.Description;
import org.sonar.api.web.RubyRailsWidget;
import org.sonar.api.web.UserRole;
import org.sonar.api.web.WidgetCategory;
import org.sonar.api.web.WidgetProperties;
import org.sonar.api.web.WidgetProperty;
import org.sonar.api.web.WidgetPropertyType;

@UserRole(UserRole.USER)
@WidgetCategory("Abacus")
@Description("Abacus")
@WidgetProperties(
{
  @WidgetProperty(key = "defaultDisplay", type = WidgetPropertyType.STRING, defaultValue = "files"),
  @WidgetProperty(key = "defaultColors", type = WidgetPropertyType.STRING, defaultValue = "33cc33,ffff33,ff9900,ff0033,000000")
})
public final class AbacusWidget extends AbstractRubyTemplate implements RubyRailsWidget {

  public String getId() {
    return "abacus";
  }

  public String getTitle() {
    return "Abacus";
  }

  @Override
  protected String getTemplatePath() {
    return "/org/sonar/plugins/abacus/abacus_widget.html.erb";
  }

}
