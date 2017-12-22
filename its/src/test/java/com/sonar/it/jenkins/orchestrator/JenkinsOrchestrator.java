/*
 * Jenkins :: Integration Tests
 * Copyright (C) 2013-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.sonar.it.jenkins.orchestrator;

import com.google.common.base.Function;
import com.sonar.it.jenkins.orchestrator.container.JenkinsDistribution;
import com.sonar.it.jenkins.orchestrator.container.JenkinsServer;
import com.sonar.it.jenkins.orchestrator.container.JenkinsWrapper;
import com.sonar.it.jenkins.orchestrator.container.ServerInstaller;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScannerInstaller;
import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.config.FileSystem;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.junit.SingleStartExternalResource;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.util.NetworkUtils;
import com.sonar.orchestrator.version.Version;
import hudson.cli.CLI;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.wsclient.jsonsimple.JSONValue;
import org.sonar.wsclient.qualitygate.QualityGates;

import static org.fest.assertions.Assertions.assertThat;

public class JenkinsOrchestrator extends SingleStartExternalResource {
  private static final Logger LOG = LoggerFactory.getLogger(JenkinsOrchestrator.class);

  private static final By MAVEN_POST_BUILD_LABEL = By.linkText("SonarQube analysis with Maven");

  public static final String DEFAULT_SONARQUBE_INSTALLATION = "SonarQube";

  private final Configuration config;
  private final JenkinsDistribution distribution;
  private final AtomicBoolean started = new AtomicBoolean(false);
  private JenkinsWrapper jenkinsWrapper;
  private JenkinsServer server;
  private WebDriver driver;
  private CLI cli;
  private static int tokenCounter = 0;

  public static class FailedExecutionException extends RuntimeException {
    FailedExecutionException(String message) {
      super(message);
    }
  }

  JenkinsOrchestrator(Configuration config, JenkinsDistribution distribution) {
    this.config = config;
    this.distribution = distribution;
  }

  @Override
  protected void beforeAll() {
    start();
  }

  @Override
  protected void afterAll() {
    stop();
  }

  public void start() {
    if (started.getAndSet(true)) {
      throw new IllegalStateException("Jenkins is already started");
    }

    int port = config.getInt("jenkins.container.port", 0);
    if (port <= 0) {
      port = NetworkUtils.getNextAvailablePort();
    }
    distribution.setPort(port);
    FileSystem fileSystem = config.fileSystem();
    ServerInstaller serverInstaller = new ServerInstaller(fileSystem);
    server = serverInstaller.install(distribution);
    server.setUrl(String.format("http://localhost:%d", port));

    jenkinsWrapper = new JenkinsWrapper(server, config, fileSystem.javaHome(), port);
    jenkinsWrapper.start();

    FirefoxProfile profile = new FirefoxProfile();
    // force language to be English
    profile.setPreference("intl.accept_languages", "en");
    FirefoxOptions options = new FirefoxOptions()
      .setProfile(profile);
    driver = new FirefoxDriver(options);

    driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
    driver.get(server.getUrl());

    // Fix window size for having reproducible test behavior
    driver.manage().window().setPosition(new Point(20, 20));
    driver.manage().window().setSize(new Dimension(1024, 768));
    try {
      cli = new CLI(new URL(server.getUrl()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    // Force updatecenter initialization
    driver.get(server.getUrl() + "/pluginManager");
    findElement(By.linkText("Advanced")).click();
    findElement(buttonByText("Check now")).click();
  }

  public void stop() {
    if (!started.getAndSet(false)) {
      // ignore double-stop
      return;
    }

    driver.quit();
    try {
      cli.close();
    } catch (Exception e) {
      LOG.warn("Error while stopping CLI", e);
    }

    if (jenkinsWrapper != null) {
      jenkinsWrapper.stopAndClean();
    }
  }

  public Configuration getConfiguration() {
    return config;
  }

  public JenkinsServer getServer() {
    return server;
  }

  public JenkinsDistribution getDistribution() {
    return distribution;
  }

  public CLI getCli() {
    return cli;
  }

  /**
   * Use environment variables and system properties
   */
  public static JenkinsOrchestrator createEnv(Version sqJenkinsPluginVersion) {
    return new JenkinsOrchestratorBuilder(Configuration.createEnv()).build();
  }

  public static JenkinsOrchestratorBuilder builderEnv() {
    return new JenkinsOrchestratorBuilder(Configuration.createEnv());
  }

  public static JenkinsOrchestratorBuilder builder(Configuration config) {
    return new JenkinsOrchestratorBuilder(config);
  }

  public JenkinsOrchestrator newMavenJob(String jobName, File projectPath) {
    newMavenJobConfig(jobName, projectPath);

    findElement(buttonByText("Save")).click();
    return this;
  }

  private void newMavenJobConfig(String jobName, File projectPath) {
    newJob(jobName, "hudson.maven.MavenModuleSet");
    configureScm(projectPath);
  }

  private void configureScm(File projectPath) {
    findElement(labelByText("File System")).click();
    setTextValue(findElement(By.name("_.path")), projectPath.getAbsolutePath());
  }

  private void newFreestyleJobConfig(String jobName, File projectPath) {
    newJob(jobName, "hudson.model.FreeStyleProject");
    configureScm(projectPath);
  }

  private void newJob(String jobName, String type) {
    driver.get(server.getUrl() + "/newJob");
    WebElement jobNameInput = findElement(By.id("name"));
    setTextValue(jobNameInput, jobName);
    WebElement jobTypeElt = findElement(By.xpath("//input[@type='radio' and @name='mode' and @value='" + type + "']"));
    if (server.getVersion().isGreaterThan("2")) {
      // Input is not visible, should click on parent
      jobTypeElt.findElement(By.xpath("..")).click();
    } else {
      jobTypeElt.click();
    }
    emulateBlur(jobNameInput);
    WebElement okButton = findElement(By.id("ok-button"));
    // May take a while before button is clickable after selecting job type
    new WebDriverWait(driver, 10)
      .until(ExpectedConditions.elementToBeClickable(okButton))
      .click();
  }

  public JenkinsOrchestrator newMavenJobWithSonar(String jobName, File projectPath, String branch) {
    newMavenJobConfig(jobName, projectPath);

    activateSonarPostBuildMaven(branch);

    findElement(buttonByText("Save")).click();
    return this;
  }

  public JenkinsOrchestrator newFreestyleJobWithSonar(String jobName, File projectPath, String branch) {
    newFreestyleJobConfig(jobName, projectPath);

    findElement(buttonByText("Add build step")).click();
    findElement(By.linkText("Invoke top-level Maven targets")).click();
    setTextValue(findElement(By.name("_.targets")), "clean package");

    activateSonarPostBuildMaven(branch);

    findElement(buttonByText("Save")).click();
    return this;
  }

  public JenkinsOrchestrator newFreestyleJobWithMaven(String jobName, File projectPath, String branch, Orchestrator orchestrator) {
    newFreestyleJobConfig(jobName, projectPath);

    findElement(By.name("hudson-plugins-sonar-SonarBuildWrapper")).click();

    findElement(buttonByText("Add build step")).click();
    findElement(By.linkText("Invoke top-level Maven targets")).click();
    setTextValue(findElement(By.name("_.targets")), "clean package");

    findElement(buttonByText("Add build step")).click();

    findElement(By.linkText("Invoke top-level Maven targets")).click();
    setTextValue(findElement(driver, By.xpath("(//input[@name='_.targets'])[2]")), getMavenParams(orchestrator));

    findElement(buttonByText("Save")).click();
    return this;
  }

  public JenkinsOrchestrator newFreestyleJobWithSQScanner(String jobName, @Nullable String additionalArgs, File projectPath, @Nullable String sqScannerVersion,
    String... properties) {
    newFreestyleJobConfig(jobName, projectPath);

    findElement(buttonByText("Add build step")).click();
    findElement(By.linkText("Execute SonarQube Scanner")).click();
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < properties.length / 2; i++) {
      String key = properties[2 * i];
      String value = properties[2 * i + 1];
      if (key.equals("sonar.task")) {
        setTextValue(findElement(By.name("_.task")), value);
      } else {
        builder.append(key).append("=").append(value).append("\n");
      }
    }
    setTextValue(findElement(By.name("_.properties")), builder.toString());

    if (sqScannerVersion != null) {
      select(findElement(By.name("sonar.sonarScannerName")), getSQScannerInstallName(sqScannerVersion));
    }

    if (additionalArgs != null) {
      setTextValue(findElement(By.name("_.additionalArguments")), additionalArgs);
    }

    findElement(buttonByText("Save")).click();
    return this;
  }

  public JenkinsOrchestrator newFreestyleJobWithScannerForMsBuild(String jobName, @Nullable String additionalArgs, File projectPath, String projectKey, String projectName,
    String projectVersion, @Nullable String msbuildScannerVersion, @Nullable String solutionFile) {
    newFreestyleJobConfig(jobName, projectPath);

    findElement(buttonByText("Add build step")).click();
    findElement(By.linkText("SonarQube Scanner for MSBuild - Begin Analysis")).click();

    setTextValue(findElement(By.name("_.projectKey")), projectKey);
    setTextValue(findElement(By.name("_.projectName")), projectName);
    setTextValue(findElement(By.name("_.projectVersion")), projectVersion);

    if (msbuildScannerVersion != null) {
      select(findElement(By.name("msBuildScannerInstallationName")), getScannerMSBuildInstallName(msbuildScannerVersion));
    }

    if (additionalArgs != null) {
      setTextValue(findElement(By.name("_.additionalArguments")), additionalArgs);
    }

    if (solutionFile != null) {
      findElement(buttonByText("Add build step")).click();
      findElement(By.linkText("Build a Visual Studio project or solution using MSBuild")).click();
      select(findElement(By.name("msBuildBuilder.msBuildName")), "MSBuild");
      setTextValue(findElement(By.name("msBuildBuilder.msBuildFile")), solutionFile);
    }

    findElement(buttonByText("Add build step")).click();
    findElement(By.linkText("SonarQube Scanner for MSBuild - End Analysis")).click();

    findElement(buttonByText("Save")).click();
    return this;
  }

  private void activateSonarPostBuildMaven(String branch) {
    WebElement addPostBuildButton = findElement(buttonByText("Add post-build action"));
    addPostBuildButton.click();
    findElement(MAVEN_POST_BUILD_LABEL).click();
    // Here we need to wait for the Sonar step to be really activated
    WebElement sonarPublisher = findElement(By.xpath("//div[@descriptorid='hudson.plugins.sonar.SonarPublisher']"));
    if (StringUtils.isNotBlank(branch)) {
      sonarPublisher.findElement(buttonByText("Advanced...")).click();
      setTextValue(sonarPublisher.findElement(By.name("sonar.branch")), branch);
    }
  }

  public String getSonarUrlOnJob(String jobName) {
    driver.get(server.getUrl() + "/job/" + jobName);
    return findElement(By.linkText("SonarQube")).getAttribute("href");
  }

  public JenkinsOrchestrator configureMavenInstallation() {
    if (config.fileSystem().mavenHome() == null) {
      throw new RuntimeException("Please configure MAVEN_HOME");
    }
    openConfigureToolsPage();

    WebElement addMavenButton = findElement(buttonByText("Add Maven"));
    addMavenButton.click();
    setTextValue(findElement(By.name("_.name")), "Maven");
    findElement(By.name("hudson-tools-InstallSourceProperty")).click();
    setTextValue(findElement(By.name("_.home")), config.fileSystem().mavenHome().getAbsolutePath());
    findElement(buttonByText("Save")).click();

    return this;
  }

  public JenkinsOrchestrator configureMSBuildInstallation() {
    if (config.getString("MSBUILD_PATH") == null) {
      throw new RuntimeException("Please configure MSBUILD_PATH");
    }
    openConfigureToolsPage();

    WebElement addMSBuildButton = findElement(buttonByText("Add MSBuild"));
    addMSBuildButton.click();
    setTextValue(findElement(By.name("_.name")), "MSBuild");
    setTextValue(findElement(By.name("_.home")), config.getString("MSBUILD_PATH"));
    findElement(buttonByText("Save")).click();

    return this;
  }

  private String getSystemConfigUrl() {
    return server.getUrl() + "/configure";
  }

  private String getGlobalToolConfigUrl() {
    return server.getUrl() + "/configureTools";
  }

  private void openConfigureToolsPage() {
    if (server.getVersion().isGreaterThan("2")) {
      driver.get(getGlobalToolConfigUrl());
    } else {
      driver.get(getSystemConfigUrl());
    }
  }

  /**
   * Scroll so that element is centered to make element visible even with Jenkins bottom/top floating bars
   */
  public WebElement scrollTo(WebElement e) {
    JavascriptExecutor js = (JavascriptExecutor) driver;
    js.executeScript(
      "const element = arguments[0]; const elementRect = element.getBoundingClientRect(); const absoluteElementTop = elementRect.top + window.pageYOffset; const top = absoluteElementTop - (window.innerHeight / 2); window.scrollTo(0, top);",
      e);
    // Give the time for the floating bar to move at the bottom
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e1) {
      e1.printStackTrace();
    }
    return e;
  }

  public void emulateBlur(WebElement e) {
    JavascriptExecutor js = (JavascriptExecutor) driver;
    js.executeScript("var obj = arguments[0];"
      + "var ev = document.createEvent('MouseEvents');"
      + "ev.initEvent('blur', true, false);"
      + "obj.dispatchEvent(ev);"
      + "return true;", e);
  }

  public JenkinsOrchestrator configureSQScannerInstallation(String version, int index) {
    openConfigureToolsPage();

    SonarScannerInstaller installer = new SonarScannerInstaller(config.fileSystem());
    File runnerScript = installer.install(Version.create(version), config.fileSystem().workspace(), true);

    String toolName = "SonarQube Scanner";

    if (index > 0) {
      findElement(buttonByText(toolName + " installations...")).click();
    }

    findElement(buttonByText("Add " + toolName)).click();
    WebElement toolArea = findElement(driver, By.xpath("(//div[@name='tool'][.//div[normalize-space(.) = '" + toolName + "']])[" + (index + 1) + "]"));
    setTextValue(findElement(toolArea, By.name("_.name")), getSQScannerInstallName(version));
    findElement(toolArea, By.name("hudson-tools-InstallSourceProperty")).click();
    WebElement homeDir = findElement(toolArea, By.name("_.home"));
    setTextValue(homeDir, runnerScript.getParentFile().getParentFile().getAbsolutePath());
    findElement(buttonByText("Save")).click();

    return this;
  }

  private String getSQScannerInstallName(String version) {
    return "SonarQube Scanner " + version;
  }

  public JenkinsOrchestrator configureMsBuildSQScanner_installation(String version, int index) {
    openConfigureToolsPage();

    String toolName = "SonarQube Scanner for MSBuild";

    if (index > 0) {
      findElement(buttonByText(toolName + " installations...")).click();
    }

    findElement(buttonByText("Add " + toolName)).click();
    WebElement toolArea = findElement(driver, By.xpath("(//div[@name='tool'][.//div[normalize-space(.) = '" + toolName + "']])[" + (index + 1) + "]"));
    setTextValue(findElement(toolArea, By.name("_.name")), getScannerMSBuildInstallName(version));
    select(findElement(toolArea, By.name("_.id")), version);
    findElement(buttonByText("Save")).click();

    return this;
  }

  private String getScannerMSBuildInstallName(String version) {
    return "Scanner for MSBuild " + version;
  }

  public JenkinsOrchestrator enableInjectionVars(boolean enable) {
    driver.get(getSystemConfigUrl());

    WebElement checkbox = findElement(By.name("enableBuildWrapper"));
    if (checkbox.isSelected() != enable) {
      checkbox.click();
    }
    findElement(buttonByText("Save")).click();
    return this;
  }

  public JenkinsOrchestrator configureSonarInstallation(Orchestrator orchestrator) {
    Version serverVersion = orchestrator.getServer().version();

    driver.get(getSystemConfigUrl());
    findElement(buttonByText("Add SonarQube")).click();

    setTextValue(findElement(By.name("sonar.name")), DEFAULT_SONARQUBE_INSTALLATION);
    setTextValue(findElement(By.name("sonar.serverUrl")), orchestrator.getServer().getUrl());
    findElement(buttonByTextAfterElementByXpath("Advanced...", "//.[@name='sonar.name']")).click();

    if (serverVersion.isGreaterThanOrEquals("5.3")) {
      String token = generateToken(orchestrator);
      select(findElement(By.className("sonar-server-version")), "5.3");
      setTextValue(findElement(By.name("sonar.serverAuthenticationToken")), token);

      assertThat(findElement(By.name("sonar.sonarLogin")).isEnabled()).isFalse();
      assertThat(findElement(By.name("sonar.sonarPassword")).isEnabled()).isFalse();
      assertThat(findElement(By.name("sonar.databaseUrl")).isEnabled()).isFalse();
      assertThat(findElement(By.name("sonar.databaseLogin")).isEnabled()).isFalse();
      assertThat(findElement(By.name("sonar.databasePassword")).isEnabled()).isFalse();

    } else if (serverVersion.isGreaterThan("5.2")) {
      select(findElement(By.className("sonar-server-version")), "5.2");
      setTextValue(findElement(By.name("sonar.sonarLogin")), Server.ADMIN_LOGIN);
      setTextValue(findElement(By.name("sonar.sonarPassword")), Server.ADMIN_PASSWORD);

      assertThat(findElement(By.name("sonar.serverAuthenticationToken")).isEnabled()).isFalse();
      assertThat(findElement(By.name("sonar.databaseUrl")).isEnabled()).isFalse();
      assertThat(findElement(By.name("sonar.databaseLogin")).isEnabled()).isFalse();
      assertThat(findElement(By.name("sonar.databasePassword")).isEnabled()).isFalse();
    } else {
      select(findElement(By.className("sonar-server-version")), "5.1");
      setTextValue(findElement(By.name("sonar.sonarLogin")), Server.ADMIN_LOGIN);
      setTextValue(findElement(By.name("sonar.sonarPassword")), Server.ADMIN_PASSWORD);
      setTextValue(findElement(By.name("sonar.databaseUrl")), orchestrator.getDatabase().getSonarProperties().get("sonar.jdbc.url"));
      setTextValue(findElement(By.name("sonar.databaseLogin")), orchestrator.getDatabase().getSonarProperties().get("sonar.jdbc.username"));
      setTextValue(findElement(By.name("sonar.databasePassword")), orchestrator.getDatabase().getSonarProperties().get("sonar.jdbc.password"));

      assertThat(findElement(By.name("sonar.serverAuthenticationToken")).isEnabled()).isFalse();
    }

    findElement(buttonByText("Save")).click();

    return this;
  }

  public void configureDefaultQG(Orchestrator orchestrator) {
    QualityGates qualityGates = orchestrator.getServer().adminWsClient().qualityGateClient().list();
    assertThat(qualityGates.qualityGates().size()).isGreaterThan(0);

    long id = qualityGates.qualityGates().iterator().next().id();
    orchestrator.getServer().adminWsClient().qualityGateClient().setDefault(id);
    System.out.println("Set default QG: " + id);
  }

  public void checkSavedSonarInstallation(Orchestrator orchestrator) {
    Version serverVersion = orchestrator.getServer().version();

    driver.get(getSystemConfigUrl());
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    assertThat(findElement(By.name("sonar.name")).getAttribute("value")).isEqualTo("SonarQube");
    assertThat(findElement(By.name("sonar.serverUrl")).getAttribute("value")).isEqualTo(orchestrator.getServer().getUrl());
    findElement(buttonByTextAfterElementByXpath("Advanced...", "//.[@name='sonar.name']")).click();

    if (serverVersion.isGreaterThanOrEquals("5.3")) {
      assertThat(findElement(By.name("sonar.serverAuthenticationToken")).getAttribute("value")).isNotEmpty();
      assertThat(findElement(By.name("sonar.sonarLogin")).isEnabled()).isFalse();
      assertThat(findElement(By.name("sonar.sonarPassword")).isEnabled()).isFalse();
      assertThat(findElement(By.name("sonar.databaseUrl")).isEnabled()).isFalse();
      assertThat(findElement(By.name("sonar.databaseLogin")).isEnabled()).isFalse();
      assertThat(findElement(By.name("sonar.databasePassword")).isEnabled()).isFalse();

    } else if (serverVersion.isGreaterThan("5.2")) {
      assertThat(findElement(By.name("sonar.sonarLogin")).getAttribute("value")).isEqualTo(Server.ADMIN_LOGIN);
      assertThat(findElement(By.name("sonar.sonarPassword")).getAttribute("value")).isEqualTo(Server.ADMIN_PASSWORD);
      assertThat(findElement(By.name("sonar.serverAuthenticationToken")).isEnabled()).isFalse();
      assertThat(findElement(By.name("sonar.databaseUrl")).isEnabled()).isFalse();
      assertThat(findElement(By.name("sonar.databaseLogin")).isEnabled()).isFalse();
      assertThat(findElement(By.name("sonar.databasePassword")).isEnabled()).isFalse();
    } else {
      assertThat(findElement(By.name("sonar.sonarLogin")).getAttribute("value")).isEqualTo(Server.ADMIN_LOGIN);
      assertThat(findElement(By.name("sonar.sonarPassword")).getAttribute("value")).isEqualTo(Server.ADMIN_PASSWORD);
      assertThat(findElement(By.name("sonar.databaseUrl")).getAttribute("value")).isEqualTo(orchestrator.getDatabase().getSonarProperties().get("sonar.jdbc.url"));
      assertThat(findElement(By.name("sonar.databaseLogin")).getAttribute("value")).isEqualTo(orchestrator.getDatabase().getSonarProperties().get("sonar.jdbc.username"));
      assertThat(findElement(By.name("sonar.databasePassword")).getAttribute("value")).isEqualTo(orchestrator.getDatabase().getSonarProperties().get("sonar.jdbc.password"));
      assertThat(findElement(By.name("sonar.serverAuthenticationToken")).isEnabled()).isFalse();
    }
  }

  public String generateToken(Orchestrator orchestrator) {
    String json = orchestrator.getServer().adminWsClient().post("api/user_tokens/generate", "name", "token" + tokenCounter++);
    Map response = (Map) JSONValue.parse(json);
    return (String) response.get("token");
  }

  public JenkinsOrchestrator installPlugin(Location hpi) {
    File hpiFile = config.fileSystem().copyToDirectory(hpi, server.getHome().getParentFile());
    if (hpiFile == null || !hpiFile.exists()) {
      throw new IllegalStateException("Can not find the plugin " + hpi);
    }
    cli.execute("install-plugin", hpiFile.getAbsolutePath(), "-deploy");
    return this;
  }

  public JenkinsOrchestrator installPlugin(String pluginKey) {
    cli.execute("install-plugin", pluginKey, "-deploy");
    return this;
  }

  public BuildResult executeJob(String jobName) {
    BuildResult result = executeJobQuietly(jobName);
    if (!result.isSuccess()) {
      throw new FailedExecutionException("Error during build of " + jobName + "\n" + result.getLogs());
    }
    return result;
  }

  public BuildResult executeJobQuietly(String jobName) {
    BuildResult result = new BuildResult();
    WriterOutputStream out = new WriterOutputStream(result.getLogsWriter());
    int exitCode = cli.execute(Arrays.asList("build", jobName, "-s", "-v"), null, out, out);
    result.addStatus(exitCode);
    return result;
  }

  private String getMavenParams(Orchestrator orchestrator) {
    Version serverVersion = orchestrator.getServer().version();

    if (serverVersion.isGreaterThanOrEquals("5.3")) {
      return "$SONAR_MAVEN_GOAL -Dsonar.host.url=$SONAR_HOST_URL -Dsonar.login=$SONAR_AUTH_TOKEN";
    } else {
      return "$SONAR_MAVEN_GOAL -Dsonar.host.url=$SONAR_HOST_URL -Dsonar.login=$SONAR_LOGIN -Dsonar.password=$SONAR_PASSWORD"
        + " -Dsonar.jdbc.url=$SONAR_JDBC_URL -Dsonar.jdbc.username=$SONAR_JDBC_USERNAME -Dsonar.jdbc.password=$SONAR_JDBC_PASSWORD";
    }
  }

  private By buttonByText(String text) {
    return By.xpath(".//button[normalize-space(.) = '" + text + "']");
  }

  private By buttonByTextAfterElementByXpath(String text, String xpath) {
    return By.xpath(xpath + "/following::button[normalize-space(.) = '" + text + "']");
  }

  private By labelByText(String text) {
    return By.xpath("//label[normalize-space(.) = '" + text + "']");
  }

  public WebDriver getDriver() {
    return driver;
  }

  public WebElement findElement(By by) {
    return findElement(driver, by);
  }

  public WebElement findElement(SearchContext context, By by) {
    try {
      return scrollTo(context.findElement(by));
    } catch (NoSuchElementException e) {
      System.err.println("Element not found. Save screenshot to: target/no_such_element.png");
      takeScreenshot(new File("target/no_such_element.png"));
      throw e;
    }
  }

  public void assertNoElement(By by) {
    List<WebElement> elements = driver.findElements(by);
    if (!elements.isEmpty()) {
      System.err.println("Not expecting finding element, but found " + elements.size() + ". Save screenshot to: target/no_such_element.png");
      takeScreenshot(new File("target/no_such_element.png"));
    }
  }

  public void takeScreenshot(File toFile) {
    File tmp = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
    try {
      FileUtils.copyFile(tmp, toFile);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Used to fix false positive about strange chars added by sendKey
   * http://code.google.com/p/selenium/issues/detail?id=4446
   * Update: it is no more necessary as I have disabled native events but I keep it just in case
   */
  public void setTextValue(final WebElement element, final String text) {
    (new WebDriverWait(driver, 10)).until(new Function<WebDriver, Boolean>() {
      @Override
      public Boolean apply(WebDriver input) {
        element.clear();
        element.sendKeys(text);
        return element.getAttribute("value").equals(text);
      }
    });
  }

  public void assertNoSonarPublisher(String jobName, File projectPath) {
    try {
      newFreestyleJobConfig(jobName, projectPath);
      findElement(buttonByText("Add post-build action")).click();
      assertNoElement(MAVEN_POST_BUILD_LABEL);
      // Save to avoid alert asking if we want to leave
    } finally {
      findElement(buttonByText("Save")).click();
    }
  }

  public void select(WebElement element, String optionValue) {
    Select select = new Select(element);
    select.selectByValue(optionValue);
  }

  public void assertQGOnProjectPage(String jobName) {
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    driver.get(server.getUrl() + "/job/" + jobName);
    WebElement table = findElement(By.className("sonar-projects"));
    WebElement qg = findElement(By.className("sonar-qg"));
    assertThat(table).isNotNull();
    assertThat(qg).isNotNull();
  }
}
