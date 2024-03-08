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
# AWS ECR. See section "eks_container" of .cirrus.yml
#------------------------------------------------------------------------------

ARG CIRRUS_AWS_ACCOUNT=275878209202
FROM ${CIRRUS_AWS_ACCOUNT}.dkr.ecr.eu-central-1.amazonaws.com/base:j17-g7-latest AS tools

USER root

#==============================================================================
# Xvfb, for integration tests
#==============================================================================
RUN apt-get update && apt-get -y install xvfb wget jq

#==============================================================================
# Install nodejs
#==============================================================================
RUN apt-get install -y nodejs

#==============================================================================
# Google Chrome, for integration tests
#==============================================================================
ARG CHROME_VERSION=google-chrome-stable
RUN echo "Using Chrome version: $CHROME_VERSION" \
  && wget --no-verbose -O /tmp/chrome_amd64.deb https://dl.google.com/linux/direct/${CHROME_VERSION}_current_amd64.deb \
  # Ignore install errors; we will try and fix them below.
  && dpkg -i /tmp/chrome_amd64.deb || true \
  && apt-get install -fy

COPY its/docker/wrap_chrome_binary /opt/bin/wrap_chrome_binary
RUN /opt/bin/wrap_chrome_binary

RUN CHROME_DRIVER_URL=$(curl "https://googlechromelabs.github.io/chrome-for-testing/last-known-good-versions-with-downloads.json" \
    | jq -r '.channels.Stable.downloads.chromedriver[] | select(.platform == "linux64").url') \
  && wget --no-verbose -O /tmp/chromedriver_linux64.zip $CHROME_DRIVER_URL \
  && rm -rf /opt/selenium/chromedriver \
  && mkdir -p /opt/selenium \
  && unzip /tmp/chromedriver_linux64.zip -d /opt/selenium \
  && rm /tmp/chromedriver_linux64.zip \
  && mv /opt/selenium/chromedriver-linux64/chromedriver /usr/bin/chromedriver \
  && chmod 755 /usr/bin/chromedriver

# Install .NET
RUN wget https://packages.microsoft.com/config/ubuntu/22.04/packages-microsoft-prod.deb \
  && dpkg -i packages-microsoft-prod.deb \
  && apt-get update \
  && apt-get install -y dotnet-sdk-8.0 \
  && apt-get clean \
  && dotnet --info

USER sonarsource
