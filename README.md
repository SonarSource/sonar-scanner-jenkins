SonarQube Scanner for Jenkins
=============================

[![Quality Gate](https://next.sonarqube.com/sonarqube/api/project_badges/measure?project=org.jenkins-ci.plugins%3Asonar&metric=alert_status)](https://next.sonarqube.com/sonarqube/dashboard?id=org.jenkins-ci.plugins%3Asonar)
[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/sonar.svg)](https://plugins.jenkins.io/sonar)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/sonar.svg?color=blue)](https://plugins.jenkins.io/sonar)

This plugin allows easy integration in Jenkins projects of [SonarQube](https://www.sonarsource.com/products/sonarqube/), the open-source solution for helping developers write [Clean Code](https://www.sonarsource.com/solutions/clean-code/?utm_medium=referral&utm_source=github&utm_campaign=clean-code&utm_content=sonar-scanner-jenkins).

* [Documentation and changelog](https://docs.sonarsource.com/sonarqube/latest/analyzing-source-code/scanners/jenkins-extension-sonarqube/)
* [Issue tracking](https://sonarsource.atlassian.net/jira/software/c/projects/SONARJNKNS/issues)

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

Copyright 2007-2023 SonarSource.

Licensed under the [GNU Lesser General Public License, Version 3.0](http://www.gnu.org/licenses/lgpl.txt)
