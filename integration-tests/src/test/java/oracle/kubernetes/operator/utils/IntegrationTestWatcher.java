// Copyright (c) 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package oracle.kubernetes.operator.utils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestWatcher;

import static oracle.kubernetes.operator.BaseTest.getLeaseId;
import static oracle.kubernetes.operator.BaseTest.getProjectRoot;

/**
 * JUnit5 extension class to intercept test execution at various
 * levels and collect logs in Kubernetes cluster for all artifacts
 * in the namespace used by the tests. The tests has to tag their classes
 * with @ExtendWith(IntegrationTestWatcher.class) for the automatic log
 * collection to work.
 */
public class IntegrationTestWatcher implements
    AfterAllCallback,
    AfterEachCallback,
    AfterTestExecutionCallback,
    BeforeAllCallback,
    BeforeEachCallback,
    BeforeTestExecutionCallback,
    InvocationInterceptor,
    LifecycleMethodExecutionExceptionHandler,
    TestExecutionExceptionHandler,
    TestWatcher {

  private String className;
  private String methodName;
  private List namespaces = null;
  private static final String START_TIME = "start time";

  static String LOGS_DIR = System.getenv().getOrDefault("RESULT_ROOT",
      System.getProperty("java.io.tmpdir")) + "/diagnosticlogs";


  /**
   * Prints log messages to separate the beforeAll methods.
   * @param context the current extension context
   */
  @Override
  public void beforeAll(ExtensionContext context) {
    className = context.getRequiredTestClass().getName();
    printHeader(String.format("Starting Test Suite %s", className), "+");
    printHeader(String.format("Starting beforeAll for %s", className), "-");
  }

  /**
   * Gets called when any exception is thrown in beforeAll and collects logs.
   * @param context current extension context
   * @param throwable to handle
   * @throws Throwable in case of failures
   */
  @Override
  public void handleBeforeAllMethodExecutionException​(ExtensionContext context, Throwable throwable)
      throws Throwable {
    printHeader(String.format("BeforeAll failed %s", className), "!");
    getNamespaceList(context);
    collectLogs(context, "beforeAll");
    throw throwable;
  }

  /**
   * Prints log message to separate the beforeEach messages.
   * @param context the current extension context
   */
  @Override
  public void beforeEach(ExtensionContext context) {
    methodName = context.getDisplayName();
    printHeader(String.format("Starting beforeEach for %s", methodName), "-");
  }

  /**
   * Gets called when any exception is thrown in beforeEach and collects logs.
   * @param context current extension context
   * @param throwable to handle
   * @throws Throwable in case of failures
   */
  @Override
  public void handleBeforeEachMethodExecutionException​(ExtensionContext context, Throwable throwable)
      throws Throwable {
    printHeader(String.format("BeforeEach failed for %s", methodName), "!");
    getNamespaceList(context);
    collectLogs(context, "beforeEach");
    throw throwable;
  }

  /**
   * Prints log messages to mark the beginning of test method execution.
   * @param context the current extension context
   * @throws Exception when store interaction fails
   */

  @Override
  public void beforeTestExecution(ExtensionContext context) throws Exception {
    printHeader(String.format("Ending beforeEach for %s", methodName), "-");
    LoggerHelper.getLocal().log(Level.INFO, "About to execute ["
            + context.getDisplayName() + "] in "
            + methodName);
    getStore(context).put(START_TIME, System.currentTimeMillis());
  }

  /**
   * Prints log messages to mark the end of test method execution.
   * @param context the current extension context
   * @throws Exception when store interaction fails
   */
  @Override
  public void afterTestExecution(ExtensionContext context) throws Exception {
    long startTime = getStore(context).remove(START_TIME, long.class);
    long duration = System.currentTimeMillis() - startTime;
    LoggerHelper.getLocal().log(Level.INFO, "Finished executing ["
            + context.getDisplayName() + "] in "
            + methodName);
    LoggerHelper.getLocal().log(Level.INFO, "Method ["
            + context.getDisplayName() + "] took "
            + duration + " ms.");
  }

  private ExtensionContext.Store getStore(ExtensionContext context) {
    return context.getStore(ExtensionContext.Namespace.create(getClass(), context.getRequiredTestMethod()));
  }

  /**
   * Intercept the invocation of a @Test method.
   * Prints log messages to separate the test method logs.
   * @param invocation the invocation that is being intercepted
   * @param invocationContext  the context of the invocation that is being intercepted
   * @param context the current extension context
   * @throws Throwable in case of failures
   */
  @Override
  public void interceptTestMethod​(Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext context) throws Throwable {
    printHeader(String.format("Starting Test %s", methodName), "-");
    invocation.proceed();
  }

  /**
   * Gets called when any exception is thrown in test and collects logs.
   * @param context current extension context
   * @param throwable to handle
   * @throws Throwable in case of failures
   */
  @Override
  public void handleTestExecutionException​(ExtensionContext context, Throwable throwable)
      throws Throwable {
    printHeader(String.format("Test failed %s", methodName), "!");
    getNamespaceList(context);
    collectLogs(context, "test");
    throw throwable;
  }

  /**
   * Intercept the invocation of a @AfterEach method.
   * Prints log messages to separate the afterEach method logs.
   * @param invocation the invocation that is being intercepted
   * @param invocationContext  the context of the invocation that is being intercepted
   * @param context the current extension context
   * @throws Throwable in case of failures
   */
  @Override
  public void interceptAfterEachMethod​(InvocationInterceptor.Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext context) throws Throwable {
    printHeader(String.format("Starting afterEach for %s", methodName), "-");
    invocation.proceed();
  }

  /**
   * Prints log message to mark the end of afterEach methods.
   * @param context the current extension context
   */
  @Override
  public void afterEach(ExtensionContext context) {
    printHeader(String.format("Ending afterEach for %s", methodName), "-");
  }

  /**
   * Gets called when any exception is thrown in afterEach and collects logs.
   * @param context current extension context
   * @param throwable to handle
   * @throws Throwable in case of failures
   */
  @Override
  public void handleAfterEachMethodExecutionException​(ExtensionContext context, Throwable throwable)
      throws Throwable {
    printHeader(String.format("AfterEach failed for %s", methodName), "!");
    getNamespaceList(context);
    collectLogs(context, "afterEach");
    throw throwable;
  }

  /**
   * Called when the test method is successful.
   * @param context the current extension context
   */
  @Override
  public void testSuccessful(ExtensionContext context) {
    printHeader(String.format("Test PASSED %s", methodName), "+");
  }

  /**
   * Called when the test method fails.
   * @param context the current extension context
   * @param cause of failures throwable
   */
  @Override
  public void testFailed(ExtensionContext context, Throwable cause) {
    printHeader(String.format("Test FAILED %s", methodName), "!");
  }

  /**
   * Intercept the invocation of a @AfterAll method.
   * Prints log messages to separate the afterAll method logs.
   * @param invocation the invocation that is being intercepted
   * @param invocationContext  the context of the invocation that is being intercepted
   * @param context the current extension context
   * @throws Throwable in case of failures
   */
  @Override
  public void interceptAfterAllMethod​(InvocationInterceptor.Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext context) throws Throwable {
    printHeader(String.format("Starting afterAll for %s", className), "-");
    invocation.proceed();
  }

  /**
   * Prints log message to mark end of test suite.
   * @param context the current extension context
   */
  @Override
  public void afterAll(ExtensionContext context) {
    printHeader(String.format("Ending Test Suite %s", className), "+");
    try {
      TestUtils.renewK8sClusterLease(getProjectRoot(), getLeaseId());
    } catch (Exception ex) {
      LoggerHelper.getLocal().log(Level.SEVERE, ex.getMessage());
    }
    LoggerHelper.getLocal().log(Level.INFO, "Starting cleanup after test class");
    getNamespaceList(context);
    CleanupUtil.cleanup(namespaces);
  }


  /**
   * Gets called when any exception is thrown in afterAll and collects logs.
   * @param context current extension context
   * @param throwable to handle
   * @throws Throwable in case of failures
   */
  @Override
  public void handleAfterAllMethodExecutionException​(ExtensionContext context, Throwable throwable)
      throws Throwable {
    printHeader(String.format("AfterAll failed for %s", className), "!");
    getNamespaceList(context);
    collectLogs(context, "afterAll");
  }

  /**
   * Collects logs in namespaces used by the current running test and writes in the LOGS_DIR.
   * @param extensionContext current extension context
   * @param failedStage the stage in which the test failed
   */
  private void collectLogs(ExtensionContext extensionContext, String failedStage) {
    LoggerHelper.getLocal().log(Level.INFO, "Collecting logs...");

    if (namespaces == null || namespaces.isEmpty()) {
      LoggerHelper.getLocal().log(Level.WARNING, "Namespace list is empty, "
          + "see if the methods in the tests is(are) annotated with Namespaces(<n>)");
      return;
    }
    Path resultDir = null;
    try {
      resultDir = Files.createDirectories(Paths.get(LOGS_DIR,
              extensionContext.getRequiredTestClass().getSimpleName(),
              getExtDir(extensionContext, failedStage)));
    } catch (IOException ex) {
      LoggerHelper.getLocal().log(Level.WARNING,ex.getMessage());
    }
    for (var namespace : namespaces) {
      LoggingUtil.collectLogs((String)namespace, resultDir.toString());
    }
  }

  /**
   * Gets the extension name for the directory based on where the test failed.
   * @param failedStage the test execution failed stage
   * @return String extension directory name
   */
  private String getExtDir(ExtensionContext extensionContext, String failedStage) {
    String ext;
    switch (failedStage) {
      case "beforeEach":
      case "afterEach":
        ext = extensionContext.getRequiredTestMethod().getName() + "_" + failedStage;
        break;
      case "test":
        ext = extensionContext.getRequiredTestMethod().getName();
        break;
      default:
        ext = failedStage;
    }
    return ext;
  }

  /**
   * Print start/end/failure messages highlighted.
   * @param message to print
   * @param rc repeater string
   */
  private void printHeader(String message, String rc) {
    LoggerHelper.getLocal().log(Level.INFO, "\n" + rc.repeat(message.length())
        + "\n" + message + "\n" + rc.repeat(message.length()) + "\n");
  }

  private void getNamespaceList(ExtensionContext context) {
    try {
      Object test = context.getRequiredTestInstance();
      Field declaredField = test.getClass().getDeclaredField("namespaceList");
      declaredField.setAccessible(true);
      StringBuffer list = (StringBuffer)declaredField.get(test);
      namespaces = Arrays.asList(list.toString().split("\\s+"));
    } catch (NoSuchFieldException | SecurityException
        | IllegalArgumentException | IllegalAccessException ex) {
      Logger.getLogger(IntegrationTestWatcher.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
}
