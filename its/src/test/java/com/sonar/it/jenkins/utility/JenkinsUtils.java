/*
 * Jenkins :: Integration Tests
 * Copyright (C) 2013-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package com.sonar.it.jenkins.utility;

import com.google.common.base.Function;
import com.sonar.it.jenkins.FilesystemScm;
import com.sonar.it.jenkins.MSBuildScannerInstallation;
import com.sonar.it.jenkins.SonarScannerInstallation;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jenkinsci.test.acceptance.plugins.credentials.CredentialsPage;
import org.jenkinsci.test.acceptance.plugins.credentials.ManagedCredentials;
import org.jenkinsci.test.acceptance.plugins.credentials.StringCredentials;
import org.jenkinsci.test.acceptance.plugins.maven.MavenInstallation;
import org.jenkinsci.test.acceptance.plugins.maven.MavenModuleSet;
import org.jenkinsci.test.acceptance.plugins.msbuild.MSBuildInstallation;
import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.Control;
import org.jenkinsci.test.acceptance.po.FreeStyleJob;
import org.jenkinsci.test.acceptance.po.Jenkins;
import org.jenkinsci.test.acceptance.po.JenkinsConfig;
import org.jenkinsci.test.acceptance.po.Job;
import org.jenkinsci.test.acceptance.po.PageAreaImpl;
import org.jenkinsci.test.acceptance.po.PageObject;
import org.jenkinsci.test.acceptance.po.ToolInstallation;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.sonarqube.ws.Qualitygates;
import org.sonarqube.ws.UserTokens;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.qualitygates.ListRequest;
import org.sonarqube.ws.client.usertokens.GenerateRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jenkinsci.test.acceptance.po.CapybaraPortingLayer.by;

public class JenkinsUtils {
  private static final By MAVEN_POST_BUILD_LABEL = buttonByText("SonarQube analysis with Maven");

  public static final String DEFAULT_SONARQUBE_INSTALLATION = "SonarQube";

  private static final String CODE_MIRROR_SCRIPT = "cmElem = document.evaluate(" +
    "        arguments[0], document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null" +
    ").singleNodeValue;" +
    "codemirror = cmElem.CodeMirror;" +
    "if (codemirror == null) {" +
    "    console.log('CodeMirror object not found!');" +
    "}" +
    "codemirror.setValue(arguments[1]);" +
    "codemirror.save();";

  private final Jenkins jenkins;
  private final WebDriver driver;
  private static int tokenCounter = 0;

  public static class FailedExecutionException extends RuntimeException {
    FailedExecutionException(String message) {
      super(message);
    }
  }

  public JenkinsUtils(Jenkins jenkins, WebDriver driver) {
    this.jenkins = jenkins;
    this.driver = driver;
  }

  public JenkinsUtils newMavenJobConfig(String jobName, File projectPath) {
    MavenModuleSet job = jenkins.jobs.create(MavenModuleSet.class, jobName);
    job.configure();
    configureScm(job, projectPath);
    return this;
  }

  private void configureScm(Job job, File projectPath) {
    job.useScm(FilesystemScm.class).scmPath(projectPath.getAbsolutePath());
  }

  public JenkinsUtils newFreestyleJobConfig(String jobName, File projectPath) {
    FreeStyleJob job = jenkins.jobs.create(FreeStyleJob.class, jobName);
    job.configure();
    configureScm(job, projectPath);
    return this;
  }

  public JenkinsUtils addMavenBuildStep(String mvnGoals) {
    findElement(buttonByText("Add build step")).click();
    findElement(buttonByText("Invoke top-level Maven targets")).click();
    setTextValue(findElement(By.name("_.targets")), mvnGoals);
    return this;
  }

  public JenkinsUtils addSonarMavenBuildStep(Orchestrator orchestrator) {
    findElement(buttonByText("Add build step")).click();
    findElement(buttonByText("Invoke top-level Maven targets")).click();
    setTextValue(findElement(driver, By.xpath("(//input[@name='_.targets'])[2]")), getMavenParams(orchestrator));
    return this;
  }

  public JenkinsUtils configureSonarBuildWrapper() {
    findElement(By.name("hudson-plugins-sonar-SonarBuildWrapper")).findElement(By.xpath("..")).click();
    return this;
  }

  public void save() {
    findElement(buttonByText("Save")).click();
  }

  public JenkinsUtils addSonarScannerBuildStep(String additionalArgs, String sqScannerVersion,
    String... properties) {
    findElement(buttonByText("Add build step")).click();
    findElement(buttonByText("Execute SonarQube Scanner")).click();
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < properties.length / 2; i++) {
      String key = properties[2 * i];
      String value = properties[2 * i + 1];
      builder.append(key).append("=").append(value).append("\n");
    }
    setTextValue(findElement(By.name("_.properties")), builder.toString());

    if (sqScannerVersion != null) {
      select(findElement(By.name("sonar.sonarScannerName")), SonarScannerInstallation.getInstallName(sqScannerVersion));
    }

    if (additionalArgs != null) {
      setTextValue(findElement(By.name("_.additionalArguments")), additionalArgs);
    }
    return this;
  }

  public JenkinsUtils newFreestyleJobWithScannerForMsBuild(String jobName, String additionalArgs, File projectPath, String projectKey, String projectName,
    String projectVersion, String msbuildScannerVersion, String solutionFile, Boolean isDotNetCore) {
    newFreestyleJobConfig(jobName, projectPath);

    findElement(buttonByText("Add build step")).click();
    findElement(buttonByText("SonarScanner for MSBuild - Begin Analysis")).click();

    setTextValue(findElement(By.name("_.projectKey")), projectKey);
    setTextValue(findElement(By.name("_.projectName")), projectName);
    setTextValue(findElement(By.name("_.projectVersion")), projectVersion);

    if (msbuildScannerVersion != null) {
      select(findElement(By.name("msBuildScannerInstallationName")), MSBuildScannerInstallation.getInstallName(msbuildScannerVersion, isDotNetCore));
    }

    if (additionalArgs != null) {
      setTextValue(findElement(By.name("_.additionalArguments")), additionalArgs);
    }

    if (solutionFile != null) {
      findElement(buttonByText("Add build step")).click();
      if (isDotNetCore) {
        if (SystemUtils.IS_OS_WINDOWS) {
          findElement(buttonByText("Execute Windows batch command")).click();
        } else {
          findElement(buttonByText("Execute shell")).click();
        }
        String command = "dotnet build " + solutionFile;
        try {
          // https://github.com/jenkinsci/acceptance-test-harness/blob/92a8ad674454f65ee105d1bbd9685be1d084e893/src/main/java/org/jenkinsci/test/acceptance/po/ShellBuildStep.java#L18
          setTextValue(findElement(By.name("command")), command);
        } catch (Exception notfound) {
          driver.findElement(By.xpath("//*[@path='/builder[1]/command']")); // wait until the element in question appears in DOM
          try {
            ((JavascriptExecutor) driver).executeScript(CODE_MIRROR_SCRIPT, "//*[@path='/builder[1]/command']/following-sibling::div", command);
          } catch (JavascriptException e) {
            System.err.println("CodeMirror not found. Save screenshot to: target/codemirror.png");
            takeScreenshot(new File("target/codemirror.png"));
            throw e;
          }
        }
      } else {
        findElement(buttonByText("Build a Visual Studio project or solution using MSBuild")).click();
        select(findElement(By.name("msBuildBuilder.msBuildName")), "MSBuild");
        setTextValue(findElement(By.name("msBuildBuilder.msBuildFile")), solutionFile);
      }
    }

    findElement(buttonByText("Add build step")).click();
    findElement(buttonByText("SonarScanner for MSBuild - End Analysis")).click();

    findElement(buttonByText("Save")).click();
    return this;
  }

  public JenkinsUtils activateSonarPostBuildMaven() {
    return activateSonarPostBuildMaven(null);
  }

  public JenkinsUtils activateSonarPostBuildMaven(String branch) {
    WebElement addPostBuildButton = findElement(buttonByText("Add post-build action"));
    addPostBuildButton.click();
    findElement(MAVEN_POST_BUILD_LABEL).click();
    // Here we need to wait for the Sonar step to be really activated
    WebElement sonarPublisher = findElement(By.xpath("//div[@descriptorid='hudson.plugins.sonar.SonarPublisher']"));
    if (StringUtils.isNotBlank(branch)) {
      sonarPublisher.findElement(buttonByText("Advanced")).click();
      setTextValue(sonarPublisher.findElement(By.name("sonar.branch")), branch);
    }
    return this;
  }

  public String getSonarUrlOnJob(String jobName) {
    jenkins.jobs.get(FreeStyleJob.class, jobName).open();
    return findElement(By.linkText("SonarQube")).getAttribute("href");
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

  public JenkinsUtils enableInjectionVars(boolean enable) {
    jenkins.configure();

    WebElement checkbox = findElement(By.name("enableBuildWrapper"));
    // get parent of current element as it is clickable
    WebElement clickable = checkbox.findElement(By.xpath(".."));
    if (checkbox.isSelected() != enable) {
      clickable.click();
    }
    findElement(buttonByText("Save")).click();
    return this;
  }

  public JenkinsUtils configureSonarInstallation(Orchestrator orchestrator) {
    return configureSonarInstallation(orchestrator, orchestrator.getServer().getUrl());
  }

  public JenkinsUtils configureSonarInstallation(Orchestrator orchestrator, String serverUrl) {

    addCredential("sonarqube-token", generateToken(orchestrator));
    addCredential("global_webhook_secret", "very_secret_secret");
    addCredential("local_webhook_secret", "super_secret_secret");
    JenkinsConfig config = jenkins.getConfigPage();
    config.open();

    final Control button = config.control(by.button("Add SonarQube"));

    String pathPrefix = button.resolve().getAttribute("path").replaceAll("repeatable-add", "inst");
    String path = config.createPageArea(pathPrefix, button::click);

    SonarQubeServer sonarQubeServer = new SonarQubeServer(config, path);
    sonarQubeServer.name.sendKeys(DEFAULT_SONARQUBE_INSTALLATION);
    sonarQubeServer.serverUrl.sendKeys(serverUrl);

    sonarQubeServer.advanced.click();

    sonarQubeServer.credentialId.select("sonarqube-token");
    if (orchestrator.getServer().version().isGreaterThanOrEquals(7, 8)) {
      sonarQubeServer.webhookSecretId.select("global_webhook_secret");
    }

    config.save();

    return this;
  }

  private void addCredential(String id, String value) {
    CredentialsPage mc = new CredentialsPage(jenkins, ManagedCredentials.DEFAULT_DOMAIN);
    mc.open();
    StringCredentials cred = mc.add(StringCredentials.class);
    cred.scope.select("GLOBAL");
    cred.secret.set(value);
    cred.setId(id);
    try {
      mc.create();
    } catch (StaleElementReferenceException e) {
      // See SONARJNKNS-387
      System.out.println("Ignore a stale element exception when adding a new credential: " + e.getMessage());
    } catch (WebDriverException e) {
      // See SONARJNKNS-387
      System.out.println("Ignore 'Node with given id does not belong to the document' exception when adding a new credential: " + e.getMessage());
    }
  }

  public static class SonarQubeServer extends PageAreaImpl {

    public final Control name = control("name");
    public final Control serverUrl = control("serverUrl");
    public final Control credentialId = control("credentialsId");
    public final Control webhookSecretId = control("webhookSecretId");
    public final Control advanced = control(by.button("Advanced"));

    public SonarQubeServer(PageObject context, String path) {
      super(context, path);
    }

  }

  public void configureDefaultQG(Orchestrator orchestrator) {
    WsClient wsClient = createWsClient(orchestrator);
    Qualitygates.ListWsResponse qualityGates = wsClient.qualitygates().list(new ListRequest());
    assertThat(qualityGates.getQualitygatesList().size()).isPositive();

    String name = qualityGates.getQualitygates(0).getName();
    wsClient.wsConnector().call(
      new PostRequest("api/qualitygates/set_as_default").setParam("name", name));

    System.out.println("Set default QG: " + name);
  }

  public String generateToken(Orchestrator orchestrator) {
    WsClient wsClient = createWsClient(orchestrator);
    UserTokens.GenerateWsResponse generate = wsClient.userTokens().generate(new GenerateRequest().setName("token" + tokenCounter++));
    return generate.getToken();
  }

  private WsClient createWsClient(Orchestrator orchestrator) {
    return WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
      .url(orchestrator.getServer().getUrl())
      .credentials(Server.ADMIN_LOGIN, Server.ADMIN_PASSWORD)
      .build());
  }

  public Build executeJob(String jobName) {
    Build result = executeJobQuietly(jobName);
    if (!result.isSuccess()) {
      throw new FailedExecutionException("Error during build of " + jobName + "\n" + result.getConsole());
    }
    return result;
  }

  public Build executeJobQuietly(String jobName) {
    Job job = jenkins.getJobs().get(Job.class, jobName);
    return job.scheduleBuild().waitUntilFinished(240);
  }

  private String getMavenParams(Orchestrator orchestrator) {
    Version serverVersion = orchestrator.getServer().version();

    if (serverVersion.isGreaterThanOrEquals(5, 3)) {
      return "$SONAR_MAVEN_GOAL -Dsonar.host.url=$SONAR_HOST_URL -Dsonar.login=$SONAR_AUTH_TOKEN";
    } else {
      return "$SONAR_MAVEN_GOAL -Dsonar.host.url=$SONAR_HOST_URL -Dsonar.login=$SONAR_LOGIN -Dsonar.password=$SONAR_PASSWORD"
        + " -Dsonar.jdbc.url=$SONAR_JDBC_URL -Dsonar.jdbc.username=$SONAR_JDBC_USERNAME -Dsonar.jdbc.password=$SONAR_JDBC_PASSWORD";
    }
  }

  private static By buttonByText(String text) {
    return By.xpath(".//button[normalize-space(.) = '" + text + "']");
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
    (new WebDriverWait(driver, Duration.ofSeconds(10))).until(new Function<WebDriver, Boolean>() {
      @Override
      public Boolean apply(WebDriver input) {
        element.clear();
        element.sendKeys(text);
        return element.getAttribute("value").equals(text);
      }
    });
  }

  public void select(WebElement element, String optionValue) {
    Select select = new Select(element);
    select.selectByValue(optionValue);
  }

  public void assertPassedQGOnProjectPage(String jobName) {
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    Job job = jenkins.getJobs().get(Job.class, jobName);
    job.open();
    WebElement table = findElement(By.className("sonar-projects"));
    WebElement qg = findElement(By.className("sonar-qg"));
    assertThat(table).isNotNull();
    assertThat(qg).isNotNull();

    WebElement status = findElement(qg, By.className("sonar-qg-status"));
    assertThat(status).isNotNull();
    assertThat(findElement(status, By.className("badge-success"))).isNotNull();
  }

  public JenkinsUtils configureMSBuild(Orchestrator orchestrator) {
    String msbuildPath = orchestrator.getConfiguration().getString("MSBUILD_PATH");
    if (msbuildPath == null) {
      throw new RuntimeException("Please configure MSBUILD_PATH");
    }
    MSBuildInstallation msbuild = ToolInstallation.addTool(jenkins, MSBuildInstallation.class);
    msbuild.name.set("MSBuild");
    msbuild.installedIn(msbuildPath);
    msbuild.getPage().save();
    return this;
  }

  public JenkinsUtils configureMaven(Orchestrator orchestrator) {
    File mavenHome = orchestrator.getConfiguration().fileSystem().mavenHome();
    if (mavenHome == null) {
      throw new RuntimeException("Please configure MAVEN_HOME");
    }
    MavenInstallation maven = ToolInstallation.addTool(jenkins, MavenInstallation.class);
    maven.name.set("Maven local");
    maven.installedIn(mavenHome.getAbsolutePath());
    maven.getPage().save();
    return this;
  }
}
