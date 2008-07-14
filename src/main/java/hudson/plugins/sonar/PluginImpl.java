package hudson.plugins.sonar;

import hudson.Plugin;
import hudson.tasks.BuildStep;

public class PluginImpl extends Plugin {
  public void start() throws Exception {
   BuildStep.PUBLISHERS.addRecorder(SonarPublisher.DESCRIPTOR);
  }
}
