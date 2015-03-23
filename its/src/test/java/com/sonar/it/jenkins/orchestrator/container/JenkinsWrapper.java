/*
 * Copyright (C) 2013-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.jenkins.orchestrator.container;

import com.google.common.annotations.VisibleForTesting;
import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.coverage.JaCoCoArgumentsBuilder;
import hudson.cli.CLI;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.SocketException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

public class JenkinsWrapper {
  private static final Logger LOG = LoggerFactory.getLogger(JenkinsWrapper.class);

  private static final int STARTUP_TIMEOUT_IN_SECONDS = 300;
  private static final String[] JARS = {"jar"};

  private final File workingDir;
  private final Configuration config;
  private final File javaHome;
  private final ExecuteWatchdog watchDog;
  private DefaultExecuteResultHandler resultHandler;
  private final DefaultExecutor executor;
  private final AtomicBoolean started = new AtomicBoolean(false);
  private final int port;

  private JenkinsServer server;

  public JenkinsWrapper(JenkinsServer server, Configuration config, File javaHome, int port) {
    this.server = server;
    this.workingDir = server.getHome();
    this.config = config;
    this.javaHome = javaHome;
    this.port = port;
    this.watchDog = createWatchDog();
    this.executor = createExecutor(watchDog);
  }

  @VisibleForTesting
  ExecuteWatchdog createWatchDog() {
    return new ExecuteWatchdog(60L * 60 * 1000);
  }

  @VisibleForTesting
  DefaultExecutor createExecutor(ExecuteWatchdog watchDog) {
    DefaultExecutor newExecutor = new DefaultExecutor();
    newExecutor.setWatchdog(watchDog);
    newExecutor.setProcessDestroyer(new ShutdownHookProcessDestroyer());
    return newExecutor;
  }

  public void start() {
    if (started.getAndSet(true)) {
      throw new IllegalStateException("App server is already started");
    }

    LOG.info("Start jenkins on port " + port + " in " + workingDir);
    resultHandler = createResultHandler();
    try {
      FileUtils.forceMkdir(workingDir);
      CommandLine command;
      if (javaHome == null) {
        command = new CommandLine("java");
      } else {
        command = new CommandLine(FileUtils.getFile(javaHome, "bin", "java"));
      }
      command.addArgument("-Xmx512M");
      command.addArgument("-XX:MaxPermSize=128m");
      command.addArgument("-Djava.awt.headless=true");
      command.addArgument("-DJENKINS_HOME=" + workingDir.getAbsolutePath());
      String jaCoCoArgument = JaCoCoArgumentsBuilder.getJaCoCoArgument(config);
      if (jaCoCoArgument != null) {
        LOG.info("JaCoCo enabled");
        command.addArgument(jaCoCoArgument);
      }

      command.addArgument("-jar");
      command.addArgument("jenkins-war-" + server.getVersion() + ".war");
      command.addArgument("--httpPort=" + port);
      command.addArgument("--ajp13Port=-1");

      executor.setWorkingDirectory(workingDir.getParentFile());
      executor.execute(command, resultHandler);
    } catch (IOException e) {
      throw new IllegalStateException("Can't start jenkins", e);
    }

    waitForJenkinsToStart();
  }

  @VisibleForTesting
  DefaultExecuteResultHandler createResultHandler() {
    return new DefaultExecuteResultHandler();
  }

  private void waitForJenkinsToStart() {
    LOG.info("Wait for jenkins to start");

    for (int i = 0; i < STARTUP_TIMEOUT_IN_SECONDS; i++) {
      try {
        if (waitForJenkinsToStart(port)) {
          LOG.info("Jenkins is started");
          return;
        }
      } catch (IllegalStateException e) {
        LOG.error("Can't start jenkins.");
        stopAndClean();
        throw new IllegalStateException("Can't start jenkins.", e);
      }
    }

    stopAndClean();
    throw new IllegalStateException("Can't start jenkins in timely fashion.");
  }

  private boolean waitForJenkinsToStart(int port) {
    try {
      URL url = new URL(String.format("http://localhost:%d", port));
      String page = IOUtils.toString(url);
      if (!page.contains("Your browser will reload automatically when Jenkins is ready.")) {
        return true;
      }
    } catch (Exception e) {
      failInCaseOfError(e);

      LOG.debug("Waiting for Jenkins to start");
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e1) {
        return false;
      }
    }

    return false;
  }

  private void failInCaseOfError(Exception e) {
    if (StringUtils.contains(e.getMessage(), "HTTP error: 5")) {
      throw new IllegalStateException(e);
    }
    if ((resultHandler.hasResult()) && (resultHandler.getException() != null)) {
      throw new IllegalStateException(resultHandler.getException());
    }
    if ((resultHandler.hasResult()) && (resultHandler.getExitValue() != 0)) {
      throw new IllegalStateException();
    }
  }

  public void stop() {
    try {
      if (!started.getAndSet(false)) {
        // already stopped
        return;
      }

      LOG.info("Stopping jenkins");
      attemptShutdown();
      resultHandler.waitFor(5 * 1000);
      if (!resultHandler.hasResult()) {
        LOG.warn("Unable to stop sonar gracefully in 5 seconds. Trying to kill process...");
      }
      watchDog.destroyProcess();
      resultHandler.waitFor();
      resultHandler = null;
      LOG.info("Jenkins is stopped");
    } catch (Exception e) {
      LOG.error("Can't stop jenkins", e);
    }
  }

  @VisibleForTesting
  void attemptShutdown() throws IOException {
    try {
      URL url = new URL(server.getUrl());
      CLI cli = new CLI(url);
      cli.execute("safe-shutdown");
      cli.close();
    } catch (SocketException e) {
      LOG.debug("Not running");
      // Okay - the server is not running
    } catch (InterruptedException e) {
      LOG.warn("Error while stopping Jenkins", e);
    }
  }

  public void stopAndClean() {
    stop();
    deleteWorkspace();
  }

  private void deleteWorkspace() {
    String value = config.getString("orchestrator.keepWorkspace", "false");
    boolean keepWorkspace = (StringUtils.isNotBlank(value) ? Boolean.valueOf(value) : false);

    if (!keepWorkspace && workingDir != null && workingDir.exists()) {
      for (File dir : workingDir.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY)) {
        if (!"jobs".equals(dir.getName())) {
          FileUtils.deleteQuietly(dir);
        }
      }
    }
  }

}
