#------------------------------------------------------------------------------
# Prepares a folder for storing Jenkins builds, which is needed to run the ITs
# during the QA task.
#
# Build from the basedir:
#   docker build -f its/docker/Dockerfile -t sonar-scanner-jenkins-qa its/docker
#
# Verify the content of the image by running a shell session in it:
#   docker run -it sonar-scanner-jenkins-qa bash
#
# CirrusCI builds the image when needed. No need to manually upload it to
# Google Cloud Container Registry. See section "gke_container" of .cirrus.yml
#------------------------------------------------------------------------------

FROM us.gcr.io/sonarqube-team/base:j11-m3-latest

USER root

#==============================================================================
# Xvfb, for integration tests
#==============================================================================
RUN apt-get update && apt-get -y install xvfb wget

#==============================================================================
# Install nodejs
#==============================================================================
RUN apt-get install -y nodejs

#==============================================================================
# Google Chrome, for integration tests
#==============================================================================
ARG CHROME_VERSION=85.0.4183.102-1
RUN echo "Using Chrome version: $CHROME_VERSION" \
  && wget --no-verbose -O /tmp/chrome_amd64.deb http://dl.google.com/linux/chrome/deb/pool/main/g/google-chrome-stable/google-chrome-stable_${CHROME_VERSION}_amd64.deb \
  # Ignore install errors; we will try and fix them below.
  && dpkg -i /tmp/chrome_amd64.deb || true
RUN apt-get install -fy

COPY its/docker/wrap_chrome_binary /opt/bin/wrap_chrome_binary
RUN /opt/bin/wrap_chrome_binary

ARG CHROME_DRIVER_VERSION=google-chrome-stable
RUN INSTALLED_CHROME_VERSION=$(apt-cache policy google-chrome-stable | grep Installed | cut -d: -f2 | xargs | cut -d\.  -f1) \
  && CHROME_DRIVER_VERSION=$(curl "https://chromedriver.storage.googleapis.com/LATEST_RELEASE_$INSTALLED_CHROME_VERSION") \
  && wget --no-verbose -O /tmp/chromedriver_linux64.zip "https://chromedriver.storage.googleapis.com/$CHROME_DRIVER_VERSION/chromedriver_linux64.zip" \
  && rm -rf /opt/selenium/chromedriver \
  && mkdir -p /opt/selenium \
  && unzip /tmp/chromedriver_linux64.zip -d /opt/selenium \
  && rm /tmp/chromedriver_linux64.zip \
  && mv /opt/selenium/chromedriver /usr/bin/chromedriver \
  && chmod 755 /usr/bin/chromedriver

RUN echo "Installing dotnet 2.2" \
  && set -x \
  && wget --no-verbose -O /tmp/dotnet.tar.gz https://download.visualstudio.microsoft.com/download/pr/228832ea-805f-45ab-8c88-fa36165701b9/16ce29a06031eeb09058dee94d6f5330/dotnet-sdk-2.2.401-linux-x64.tar.gz \
  && mkdir -p /usr/share/dotnet/ \
  && tar zxf /tmp/dotnet.tar.gz -C /usr/share/dotnet/ && rm /tmp/dotnet.tar.gz \
  && ln -s /usr/share/dotnet/dotnet /usr/bin/dotnet \
  && chmod -R 755 /usr/share/dotnet \
  && chmod 755 /usr/bin/dotnet
RUN dotnet --info

USER sonarsource
