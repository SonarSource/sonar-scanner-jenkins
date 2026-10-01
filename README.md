<!-- Sonar Marketing hosts these approved brand assets on its Kentico Kontent CDN (assets-eu-01.kc-usercontent.com). Shared URLs are intentional; consult Marketing before replacing them. -->
<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://assets-eu-01.kc-usercontent.com/ef593040-b591-0198-9506-ed88b30bc023/a23fc7ba-23f0-489a-829d-ed88c0748521/Sonar_Logo_Dark%20Backgrounds.svg">
    <img src="https://assets-eu-01.kc-usercontent.com/ef593040-b591-0198-9506-ed88b30bc023/82c13eba-d95c-4bb8-8007-7ce77c14e043/Sonar_Logo_Light%20Backgrounds.svg" alt="Sonar logo" width="400">
  </picture>
</p>

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="./images/SonarQube_dark.png">
  <img src="./images/SonarQube_light.png">
</picture>

[![Quality Gate](https://next.sonarqube.com/sonarqube/api/project_badges/measure?project=org.jenkins-ci.plugins%3Asonar&metric=alert_status)](https://next.sonarqube.com/sonarqube/dashboard?id=org.jenkins-ci.plugins%3Asonar)
[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/sonar.svg)](https://plugins.jenkins.io/sonar)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/sonar.svg?color=blue)](https://plugins.jenkins.io/sonar)

<!-- sonar-marketing:start -->
<!-- Marketing maintains this section. For wording changes, consult the relevant Product Marketing Manager (PMM). Repository CODEOWNERS review accuracy and merge changes. -->

# SonarQube Scanner for Jenkins

This Jenkins plugin connects Jenkins jobs and pipelines to SonarQube Server, SonarQube Cloud, or SonarQube Community Build so they can run code quality and security analysis.

Choose the relevant documentation below, or learn more about the [SonarQube product family](https://www.sonarsource.com/products/sonarqube/).

<!-- sonar-marketing:end -->

* [Issue tracking](https://sonarsource.atlassian.net/jira/software/c/projects/SONARJNKNS/issues)

Documentation and changelog:
* [SonarQube Server](https://docs.sonarsource.com/sonarqube-server/latest/analyzing-source-code/scanners/jenkins-extension-sonarqube/)
* [SonarQube Cloud](https://docs.sonarsource.com/sonarqube-cloud/advanced-setup/ci-based-analysis/sonarcloud-extension-for-jenkins/)
* [SonarQube Community Build](https://docs.sonarsource.com/sonarqube-community-build/analyzing-source-code/scanners/jenkins-extension-sonarqube/)


If you want to make changes, please clone the [Git repository](https://github.com/SonarSource/sonar-scanner-jenkins).

With this plugin, you can configure SonarQube instances and run a Sonar Scanner analysis in several ways:

* By injecting the SonarQube configuration as environment variables and using them in any job step (such as Maven, Ant, Gradle, ...)
* Using the SonarQube Scanner build step
* Using SonarScanner for MSBuild analysis steps

'SonarQube Scanner' and 'SonarScanner for MSBuild' are managed as installable tools. List of available versions is retrieved
automatically by Jenkins from a json file hosted on the update site:

* https://updates.jenkins.io/updates/hudson.plugins.sonar.SonarRunnerInstaller.json
* https://updates.jenkins.io/updates/hudson.plugins.sonar.MsBuildSonarQubeRunnerInstaller.json

The files are automatically updated when a new version of SonarScanner or SonarScanner for MSBuild is published,
thanks to crawlers written in groovy:

* https://github.com/jenkins-infra/crawler/blob/master/sonarqubescanner.groovy
* https://github.com/jenkins-infra/crawler/blob/master/sonarqubescannermsbuild.groovy

License
-------

Copyright 2007-2025 SonarSource.

Licensed under the [GNU Lesser General Public License, Version 3.0](http://www.gnu.org/licenses/lgpl.txt)
