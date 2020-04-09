// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package oracle.kubernetes.operator;

import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import oracle.kubernetes.operator.utils.LoggerHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.Alphanumeric;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

@TestMethodOrder(Alphanumeric.class)

@ExtendWith(Telemetry.class)
public class ItMark {

  Random random = new Random(System.currentTimeMillis());
  Logger logger = LoggerHelper.getLocal();

  @BeforeAll
  public static void staticPrepare() throws Exception {
    LoggerHelper.getLocal().log(Level.INFO, "before all");
  }

  @BeforeEach
  public void prepare() throws Exception {
    logger.log(Level.INFO, "before each");
  }
  
  @AfterEach
  public void unPrepare() throws Exception {
    logger.log(Level.INFO, "after each");
  }

  @AfterAll
  public static void staticUnPrepare() throws Exception {
    LoggerHelper.getLocal().log(Level.INFO, "after all");
  }
 
  @Test
  public void testSomething() {
    try {
      Thread.sleep(random.nextInt(10000)); // 10 seconds
    } catch (InterruptedException ignore) {
      // ignore
    }
    logger.log(Level.INFO, "test");
  }

  @Test
  public void testSomethingElse() {
    try {
      Thread.sleep(random.nextInt(10000)); // 10 seconds
    } catch (InterruptedException ignore) {
      // ignore
    }
    logger.log(Level.INFO, "test");
  }
}
