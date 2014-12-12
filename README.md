Jenkins SonarQube Plugin
====================

Project homepage: http://redirect.sonarsource.com/plugins/jenkins.html

Continuous inspection: http://nemo.sonarsource.org/dashboard/index/org.jvnet.hudson.plugins:sonar

Issue tracking: http://jira.codehaus.org/browse/SONARJNKNS


If you're wanting to make changes, please clone the git repository at

git://github.com/SonarSource/jenkins-sonar-plugin.git

Before making release please contact current maintainers ( dev@sonar.codehaus.org ).

SonarQube Runner is managed as an installable tool. List of available versions is retrieved
automatically by Jenkins/Hudson from a json file hosted on their respective update site:
* http://hudson-ci.org/updates/hudson.plugins.sonar.SonarRunnerInstaller.json
* http://mirrors.jenkins-ci.org/updates/updates/hudson.plugins.sonar.SonarRunnerInstaller.json

For Jenkins the file is automatically updated when a new version of Sonar Runner is published
thanks to a crawler written in groovy:
https://github.com/jenkinsci/backend-crawler/blob/master/sonarrunner.groovy

For Hudson it seems it is a manual process and we should ask on the hudson dev mailing list
for someone to update the json file.