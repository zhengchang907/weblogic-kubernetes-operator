// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package oracle.kubernetes.operator;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HttpConnectionFactory;
import io.prometheus.client.exporter.PushGateway;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class Telemetry
    implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

  private static final Logger logger = Logger.getLogger(Telemetry.class.getName());
  private static final String START_TIME = "start time";

  @Override
  public void beforeTestExecution(ExtensionContext context) throws Exception {
    getStore(context).put(START_TIME, System.currentTimeMillis());
  }

  @Override
  public void afterTestExecution(ExtensionContext context) throws Exception {
    Method testMethod = context.getRequiredTestMethod();
    Class testClass = context.getRequiredTestClass();
    long startTime = getStore(context).remove(START_TIME, long.class);
    long duration = (System.currentTimeMillis() - startTime) / 1000;

    logger.info(() ->
        String.format("Method [%s] took %s ms.", testMethod.getName(), duration));

    Boolean testFailed = context.getExecutionException().isPresent();
    if (testFailed) {
      logger.info("Test failed");
    } else {
      logger.info("Test passed");
    }

    // prometheus
    logger.info("Getting ready to send metric to prometheus...");
    CollectorRegistry registry = new CollectorRegistry();
    PushGateway pushGateway = new PushGateway("sentinel.weblogick8s.org:31191");
    pushGateway.setConnectionFactory(new MyHttpConnectionFactory());
    final Gauge testReport = Gauge.build()
        .name("wko_test_report")
        .help("Reports the status of a WebLogic Kubernetes Operator test")
        .register(registry);
    testReport.set(duration);
    Map<String, String> groupingKey = new HashMap<>();
    groupingKey.put("class", testClass.getName());
    groupingKey.put("method", testMethod.getName());
    logger.info("data: " + testReport.get() + "\ngrouping: " + groupingKey);
    pushGateway.pushAdd(registry, "wko", groupingKey);

  }

  private ExtensionContext.Store getStore(ExtensionContext context) {
    return context.getStore(ExtensionContext.Namespace.create(getClass(), context.getRequiredTestMethod()));
  }

  class MyHttpConnectionFactory implements HttpConnectionFactory {
    @Override
    public HttpURLConnection create(String url) throws IOException {

      HttpURLConnection connection = null;
      boolean oracle = false;

      // check if we are on the oracle network
      try {
        InetAddress inetAddress = InetAddress.getByName("www-proxy-hqdc.us.oracle.com");
        oracle = true;
      } catch (UnknownHostException e) {
        // we are not!
      }
      if (oracle) {
        SocketAddress addr = new
            InetSocketAddress("www-proxy-hqdc.us.oracle.com", 80);
        Proxy proxy = new Proxy(Proxy.Type.HTTP, addr);
        connection = (HttpURLConnection) new URL(url).openConnection(proxy);
      } else {
        connection = (HttpURLConnection) new URL(url).openConnection();
      }
      return connection;
    }
  }
}
