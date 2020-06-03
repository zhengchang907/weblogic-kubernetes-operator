// Copyright (c) 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package oracle.weblogic.kubernetes;

import java.util.List;

import oracle.weblogic.kubernetes.annotations.IntegrationTest;
import oracle.weblogic.kubernetes.annotations.Namespaces;
import oracle.weblogic.kubernetes.extensions.LoggedTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static oracle.weblogic.kubernetes.extensions.LoggedTest.logger;

/**
 * Tests to create domain in persistent volume using WLST and WDT.
 */
@DisplayName("Verify the WebLogic server pods can run with domain created in persistent volume")
@IntegrationTest
public class ItCleanup implements LoggedTest {

  @BeforeAll
  public static void initAll(@Namespaces(1) List<String> namespaces) {
    logger.info("Got namespace {0}", namespaces.get(0));
  }

  @Test
  public void cleanup() {
    logger.info("Running cleanup");
  }

}
