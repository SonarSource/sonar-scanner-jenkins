/*
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package hudson.plugins.sonar;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.model.Executor;
import hudson.model.JDK;
import hudson.model.Node;
import hudson.util.ArgumentListBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;

/**
 * Allows to execute embedded sonar-runner.
 * 
 * @since 1.7
 */
public class SonarRunner {

  private final AbstractProject project;
  private final Launcher launcher;
  private final EnvVars envVars;
  private final FilePath workDir;
  private FilePath runnerJar;
  private FilePath bootstrapperJar;

  public SonarRunner(AbstractProject project, Launcher launcher, EnvVars envVars, FilePath workDir) {
    this.project = project;
    this.launcher = launcher;
    this.envVars = envVars;
    this.workDir = workDir;
  }

  public int launch(BuildListener listener, SonarRunnerBuilder conf) throws IOException, InterruptedException {
    try {
      extract();
      ArgumentListBuilder args = buildCmdLine(listener, conf);
      return launcher.launch().cmds(args).envs(envVars).stdout(listener).pwd(workDir).join();
    } finally {
      cleanup();
    }
  }

  /**
   * Visibility of a method has been relaxed for tests.
   */
  ArgumentListBuilder buildCmdLine(BuildListener listener, SonarRunnerBuilder conf) throws IOException, InterruptedException {
    ArgumentListBuilder args = new ArgumentListBuilder();
    // Java
    args.add(getJavaExecutable(listener));
    // Java options
    args.addTokenized(envVars.expand(conf.getJavaOpts()));
    // Classpath
    args.add("-cp");
    args.add(runnerJar.getRemote() + getClasspathDelimiter() + bootstrapperJar.getRemote());
    // Main class
    args.add("org.sonar.runner.Main");
    // Server properties
    SonarInstallation si = conf.getSonarInstallation();
    if (si != null) {
      appendArg(args, "sonar.jdbc.driver", si.getDatabaseDriver());
      appendArg(args, "sonar.jdbc.url", si.getDatabaseUrl()); // TODO can be masked
      appendMaskedArg(args, "sonar.jdbc.username", si.getDatabaseLogin());
      appendMaskedArg(args, "sonar.jdbc.password", si.getDatabasePassword());
      appendArg(args, "sonar.host.url", si.getServerUrl());
    }
    // Path to project properties
    appendArg(args, "project.settings", conf.getProject());

    // Additional properties
    Properties p = new Properties();
    p.load(new ByteArrayInputStream(conf.getProperties().getBytes()));
    for (Entry<Object, Object> entry : p.entrySet()) {
      args.add("-D" + entry.getKey() + "=" + entry.getValue().toString());
    }
    return args;
  }

  private static void appendArg(ArgumentListBuilder args, String name, String value) {
    value = StringUtils.trimToEmpty(value);
    if (StringUtils.isNotEmpty(value)) {
      args.add("-D" + name + "=" + value);
    }
  }

  private static void appendMaskedArg(ArgumentListBuilder args, String name, String value) {
    value = StringUtils.trimToEmpty(value);
    if (StringUtils.isNotEmpty(value)) {
      args.addMasked("-D" + name + "=" + value);
    }
  }

  /**
   * Visibility of a method has been relaxed for tests.
   */
  void extract() throws IOException, InterruptedException {
    runnerJar = workDir.createTempFile("sonar-runner", ".jar");
    runnerJar.copyFrom(this.getClass().getClassLoader().getResource("sonar-runner.jar"));

    bootstrapperJar = workDir.createTempFile("sonar-batch-bootstrapper", ".jar");
    bootstrapperJar.copyFrom(this.getClass().getClassLoader().getResource("sonar-batch-bootstrapper.jar"));
  }

  /**
   * Visibility of a method has been relaxed for tests.
   */
  void cleanup() throws IOException, InterruptedException {
    if (runnerJar != null) {
      runnerJar.delete();
    }
    if (bootstrapperJar != null) {
      bootstrapperJar.delete();
    }
  }

  /**
   * Visibility of a method has been relaxed for tests.
   */
  char getClasspathDelimiter() {
    return launcher.isUnix() ? ':' : ';';
  }

  /**
   * @return path to Java executable to be used with this project, never <tt>null</tt>
   */
  private String getJavaExecutable(BuildListener listener) throws IOException, InterruptedException {
    JDK jdk = project.getJDK();
    if (jdk != null) {
      jdk = jdk.forNode(getCurrentNode(), listener).forEnvironment(envVars);
    }
    return jdk == null ? "java" : jdk.getHome() + "/bin/java";
  }

  /**
   * @return the current {@link Node} on which we are building
   */
  private Node getCurrentNode() {
    return Executor.currentExecutor().getOwner().getNode();
  }

}
