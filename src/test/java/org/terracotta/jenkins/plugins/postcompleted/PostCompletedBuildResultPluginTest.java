package org.terracotta.jenkins.plugins.postcompleted;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.recipes.LocalData;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;


public class PostCompletedBuildResultPluginTest extends HudsonTestCase {
  //  @Rule  public JenkinsRule j = new JenkinsRule();
  public static final int PORT = 43434;
  private static MyHandler myHandler;
  private static HttpServer httpServer;

  @BeforeClass
  public static void startServer() throws IOException {
  }

  @Test
  @LocalData
  public void test() throws Exception {
    InetSocketAddress inetSocketAddress = new InetSocketAddress(PORT);
    httpServer = HttpServer.create(inetSocketAddress, 0);

    myHandler = new MyHandler();
    httpServer.createContext("/service", myHandler);
    httpServer.setExecutor(Executors.newCachedThreadPool());
    httpServer.start();

    FreeStyleProject project = (FreeStyleProject) hudson.getItem("quickJob");
    FreeStyleBuild build = project.scheduleBuild2(0).get();
    assertBuildStatus(Result.SUCCESS, build);

    Assert.assertTrue(myHandler.requestBodyAsText.contains("url=" + hudson.getRootUrl() + "job/quickJob/5"));

  }

  class MyHandler implements HttpHandler {

    public String requestBodyAsText;

    public void handle(HttpExchange exchange) throws IOException {
      String requestMethod = exchange.getRequestMethod();
      if (requestMethod.equalsIgnoreCase("POST")) {
        Headers responseHeaders = exchange.getResponseHeaders();
//      responseHeaders.set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(201, 0);
        InputStream requestBody = exchange.getRequestBody();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(requestBody, "UTF-8"));
        String line;
        StringBuilder requestBodyBuilder = new StringBuilder();
        while ((line = bufferedReader.readLine()) != null) {
          requestBodyBuilder.append(line);
          requestBodyBuilder.append("\n");
        }

        OutputStream responseBody = exchange.getResponseBody();
        responseBody.write(new String("allGood").getBytes());
        requestBodyAsText = requestBodyBuilder.toString();
        Headers requestHeaders = exchange.getRequestHeaders();
        responseBody.close();
      }
    }
  }

  @AfterClass
  public static void stopServer() {
    httpServer.stop(0);
  }
}
