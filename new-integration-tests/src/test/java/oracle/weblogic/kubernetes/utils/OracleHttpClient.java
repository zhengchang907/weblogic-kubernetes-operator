// Copyright (c) 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package oracle.weblogic.kubernetes.utils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static oracle.weblogic.kubernetes.extensions.LoggedTest.logger;

/**
 * A simple Http client.
 */
public class OracleHttpClient {

  private static final HttpClient httpClient = HttpClient.newBuilder()
      .version(HttpClient.Version.HTTP_1_1)
      .connectTimeout(Duration.ofSeconds(30))
      .followRedirects(HttpClient.Redirect.NORMAL)
      .build();

  /**
   * Http GET request.
   * @param url URL of the web resource
   * @param debug if true prints status code and response body
   * @return HttpResponse object
   * @throws IOException when cannot connect to the URL
   * @throws InterruptedException  when connection to web resource times out
   */
  public static HttpResponse<String> get(String url, boolean debug)
      throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create(url)).build();
    logger.info("Sending http request {0}", url);
    HttpResponse<String> response = httpClient.send(request,
        HttpResponse.BodyHandlers.ofString());
    if (debug) {
      logger.info("HTTP_STATUS: {0}", response.statusCode());
      logger.info("Response Body: {0}", response.body());
    }
    return response;
  }
}
