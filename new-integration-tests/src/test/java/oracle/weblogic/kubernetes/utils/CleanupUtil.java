// Copyright (c) 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package oracle.weblogic.kubernetes.utils;

import java.util.List;

import io.kubernetes.client.openapi.ApiException;
import oracle.weblogic.kubernetes.actions.impl.primitive.Kubernetes;

import static oracle.weblogic.kubernetes.extensions.LoggedTest.logger;

/**
 * CleanupUtil is used for cleaning up all the Kubernetes artifacts left behind by the integration tests.
 *
 */
public class CleanupUtil {

  /**
   * Cleanup Kubernetes artifacts in the namespaces used by the test class.
   *
   * <p>Tries to gracefully delete any existing WebLogic domains and WebLogic Operator in the namespaces.
   * Then deletes everything in the namespaces.
   *
   * <p>Waits for the deletion task to be completed, for up to 10 minutes.
   *
   * @param namespaces list of namespaces
   */
  public static void cleanup(List<String> namespaces) {
    try {

      namespaces = Kubernetes.listNamespaces();
      logger.info(namespaces.toString());
      deleteNamespaces();
    } catch (ApiException ex) {
      logger.warning(ex.getMessage());
      logger.warning("Cleanup failed");
    }
  }

  private static void deleteNamespaces() {
    try {
      for (var namespace : Kubernetes.listNamespaces()) {
        logger.info("Namespace found : {0}", namespace);
        if (namespace.startsWith("ns-")) {
          logger.info("Deleting namespace: {0}", namespace);
          Kubernetes.deleteNamespace(namespace);
        }
      }
      logger.info("Listing Persistent Volumes");
      for (var pv : Kubernetes.listPersistentVolumes().getItems()) {
        logger.info(pv.getMetadata().getName());
      }
      logger.info("Listing Clusterrolebindings");
      for (var rb : Kubernetes.listClusterRoleBindings(null).getItems()) {
        logger.info(rb.getMetadata().getName());
      }
      logger.info("Listing Clusterroles");
      for (var r : Kubernetes.listClusterRoles(null).getItems()) {
        logger.info(r.getMetadata().getName());
      }
    } catch (ApiException ex) {
      logger.severe(ex.getResponseBody());
    }
  }

}
