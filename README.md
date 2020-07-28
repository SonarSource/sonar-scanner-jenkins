SonarQube Scanner for Jenkins
=============================

[![Build Status](https://travis-ci.org/SonarSource/sonar-scanner-jenkins.svg?branch=master)](https://travis-ci.org/SonarSource/sonar-scanner-jenkins) [![Quality Gate](https://next.sonarqube.com/sonarqube/api/project_badges/measure?project=org.jenkins-ci.plugins%3Asonar&metric=alert_status)](https://next.sonarqube.com/sonarqube/dashboard?id=org.jenkins-ci.plugins%3Asonar)

Documentation: http://redirect.sonarsource.com/plugins/jenkins.html

Issue tracking: https://jira.sonarsource.com/browse/SONARJNKNS

If you want to make changes, please clone the [Git repository](https://github.com/SonarSource/sonar-scanner-jenkins).

With this plugin, you can configure SonarQube instances and run a SonarQube Scanner analysis in several ways:
* By injecting the SonarQube configuration as environment variables and using them in any job step (such as Maven, Ant, Gradle, ...);
* Using the Sonar Scanner job;
* Using SonarScanner for MSBuild's 'begin analysis' and 'end analysis' jobs;


'SonarQube Scanner' and 'SonarScanner for MSBuild' are managed as installable tools. List of available versions is retrieved
automatically by Jenkins/Hudson from a json file hosted on their respective update site:
* http://hudson-ci.org/updates/hudson.plugins.sonar.SonarRunnerInstaller.json
* http://mirrors.jenkins-ci.org/updates/updates/hudson.plugins.sonar.SonarRunnerInstaller.json
* http://mirrors.jenkins-ci.org/updates/updates/hudson.plugins.sonar.MsBuildSonarQubeRunnerInstaller.json

For Jenkins, the files are automatically updated when a new version of Sonar Scanner or SonarScanner for MSBuild is published,
thanks to crawlers written in groovy:
* https://github.com/jenkins-infra/crawler/blob/master/sonarqubescanner.groovy
* https://github.com/jenkins-infra/crawler/blob/master/sonarqubescannermsbuild.groovy

License
-------

Copyright 2007-2020 SonarSource.

Licensed under the [GNU Lesser General Public License, Version 3.0](http://www.gnu.org/licenses/lgpl.txt)
