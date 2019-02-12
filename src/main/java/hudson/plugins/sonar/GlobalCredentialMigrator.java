/*
 * SonarQube Scanner for Jenkins
 * Copyright (C) 2007-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package hudson.plugins.sonar;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.security.ACL;
import hudson.util.Secret;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;

class GlobalCredentialMigrator {

  private static final Logger LOGGER = Logger.getLogger(GlobalCredentialMigrator.class.getName());

  StandardCredentials migrate(@Nonnull String token) {
    LOGGER.info("Migrating SonarQube credential: moving authentication token into a credential");

    List<StringCredentials> allStringCredentials = CredentialsMatchers.filter(
      CredentialsProvider.lookupCredentials(
        StringCredentials.class,
        Jenkins.getInstance(),
        ACL.SYSTEM,
        (DomainRequirement) null),
      CredentialsMatchers.always());

    return allStringCredentials
      .stream()
      .filter(cred -> cred.getSecret().getPlainText().equals(token))
      .findAny()
      .orElseGet(() -> addCredentialIfNotPresent(token));
  }

  private static StringCredentials addCredentialIfNotPresent(@Nonnull String token) {
    StringCredentials credentials = new StringCredentialsImpl(
      CredentialsScope.GLOBAL,
      UUID.randomUUID().toString(),
      "Migrated SonarQube authentication token",
      Secret.fromString(token));

    SystemCredentialsProvider instance = SystemCredentialsProvider.getInstance();
    instance.getCredentials().add(credentials);
    try {
      instance.save();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    return credentials;
  }
}
