package hudson.plugins.sonar.template;

import hudson.FilePath;
import hudson.plugins.sonar.SonarPublisher;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Evgeny Mandrikov
 */
public final class SonarPomGenerator {
  public static void generatePomForNonMavenProject(SonarPublisher publisher, FilePath root, String pomName) throws IOException, InterruptedException {
    SimpleTemplate pomTemplate = new SimpleTemplate("hudson/plugins/sonar/sonar-light-pom.template");
    pomTemplate.setAttribute("groupId", publisher.getGroupId());
    pomTemplate.setAttribute("artifactId", publisher.getArtifactId());
    pomTemplate.setAttribute("projectName", publisher.getProjectName()); // FIXME Godin: env.expand because projectName can be "${JOB_NAME}"
    pomTemplate.setAttribute("projectVersion", StringUtils.isEmpty(publisher.getProjectVersion()) ? "1.0" : publisher.getProjectVersion());
    pomTemplate.setAttribute("javaVersion", StringUtils.isEmpty(publisher.getJavaVersion()) ? "1.5" : publisher.getJavaVersion());

    List<String> srcDirs = getProjectSrcDirsList(publisher.getProjectSrcDir());
    boolean multiSources = srcDirs.size() > 1;
    setPomElement("sourceDirectory", srcDirs.get(0), true, pomTemplate);
    pomTemplate.setAttribute("srcDirsPlugin", multiSources ? generateSrcDirsPluginTemplate(srcDirs).toString() : "");

    setPomElement("project.build.sourceEncoding", publisher.getProjectSrcEncoding(), true, pomTemplate);
    setPomElement("encoding", publisher.getProjectSrcEncoding(), true, pomTemplate);
    setPomElement("description", publisher.getProjectDescription(), true, pomTemplate);
    setPomElement("sonar.phase", multiSources ? "generate-sources" : "", true, pomTemplate);
    setPomElement("outputDirectory", publisher.getProjectBinDir(), StringUtils.isNotBlank(publisher.getProjectBinDir()), pomTemplate);
    setPomElement("sonar.dynamicAnalysis", publisher.isReuseReports() ? "reuseReports" : "false", true, pomTemplate);
    setPomElement("sonar.surefire.reportsPath", publisher.getSurefireReportsPath(), publisher.isReuseReports(), pomTemplate);
    setPomElement("sonar.cobertura.reportPath", publisher.getCoberturaReportPath(), publisher.isReuseReports(), pomTemplate);
    setPomElement("sonar.clover.reportPath", publisher.getCloverReportPath(), publisher.isReuseReports(), pomTemplate);

    pomTemplate.write(root, pomName);
  }

  private static SimpleTemplate generateSrcDirsPluginTemplate(List<String> srcDirs) throws IOException, InterruptedException {
    SimpleTemplate srcTemplate = new SimpleTemplate("hudson/plugins/sonar/sonar-multi-sources.template");
    StringBuffer sourcesXml = new StringBuffer();
    for (int i = 1; i < srcDirs.size(); i++) {
      sourcesXml.append("<source><![CDATA[").append(StringUtils.trim(srcDirs.get(i))).append("]]></source>\n");
    }
    srcTemplate.setAttribute("sources", sourcesXml.toString());
    return srcTemplate;
  }

  private static void setPomElement(String tagName, String tagValue, boolean enabled, SimpleTemplate template) {
    String tagContent = enabled && StringUtils.isNotBlank(tagValue) ? "<" + tagName + "><![CDATA[" + tagValue + "]]></" + tagName + ">" : "";
    template.setAttribute(tagName, tagContent);
  }

  private static List<String> getProjectSrcDirsList(String src) {
    String[] dirs = StringUtils.split(src, ',');
    return Arrays.asList(dirs);
  }

  /**
   * Hide utility-class constructor.
   */
  private SonarPomGenerator() {
  }
}
