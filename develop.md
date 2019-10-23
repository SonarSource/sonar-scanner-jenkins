Development notes
=================

Useful notes during development.

Running Jenkins with the plugin installed
-----------------------------------------

During development, it's easiest to run Jenkins with the plugin installed with Maven:

    mvn hpi:run

The web app is at: http://localhost:8080/jenkins/

Creating a simple Jenkins job
-----------------------------

See the [online documentation](https://sonarcloud.io/documentation/analysis/scan/sonarscanner-for-jenkins/) for detailed setup steps and example pipeline scripts.

Testing the plugin in a "real" Jenkins
--------------------------------------

It's convenient to run Jenkins in Docker, for example:

    docker run \
      -u root \
      --rm \
      -p 8080:8080 \
      -v /var/run/docker.sock:/var/run/docker.sock \
      jenkinsci/blueocean

(For more details see the [Jenkins documentation](https://jenkins.io/doc/book/installing/).)

Package the plugin by running:

    mvn compile hpi:hpi

This creates the package file `target/sonar.hpi`.
On Jenkins, go to **Manage Jenkins / Manage Plugins / Advanced**,
and use the form in the **Upload Plugin** section to upload the package.
