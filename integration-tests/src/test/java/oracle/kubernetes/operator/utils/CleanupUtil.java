// Copyright (c) 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package oracle.kubernetes.operator.utils;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.ExtensionsV1beta1Ingress;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1PersistentVolume;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaim;
import io.kubernetes.client.openapi.models.V1ReplicaSet;
import io.kubernetes.client.openapi.models.V1Role;
import io.kubernetes.client.openapi.models.V1RoleBinding;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceAccount;
import org.awaitility.core.ConditionFactory;

import static io.kubernetes.client.util.Yaml.dump;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.with;

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
      // If namespace list is empty or null return
      if (namespaces == null || namespaces.isEmpty()) {
        LoggerHelper.getLocal().info("Nothing to cleanup");
        return;
      }

      // Delete artifacts in namespace used by the test class and the namespace itself
      for (var namespace : namespaces) {
        deleteNamespacedArtifacts(namespace);
        deleteNamespace(namespace);
      }

      // Using Thread.sleep for a one time 30 sec sleep.
      // If pollDelay is set to 30 seconds below, its going to sleep 30 seconds for every namespace.
      try {
        Thread.sleep(30 * 1000);
      } catch (InterruptedException e) {
        //ignore
      }

      // wait for the artifacts to be deleted, waiting for a maximum of 10 minutes
      ConditionFactory withStandardRetryPolicy = with().pollDelay(0, SECONDS)
          .and().with().pollInterval(10, SECONDS)
          .atMost(10, MINUTES).await();

      for (var namespace : namespaces) {
        LoggerHelper.getLocal().info("Check for artifacts in namespace " + namespace);
        withStandardRetryPolicy
            .conditionEvaluationListener(
                condition -> LoggerHelper.getLocal().info("Waiting for artifacts to be deleted in namespace"))
            .until(nothingFoundInNamespace(namespace));

        LoggerHelper.getLocal().info("Check for namespace " + namespace + " existence");
        withStandardRetryPolicy
            .conditionEvaluationListener(
                condition -> LoggerHelper.getLocal().info("Waiting for namespace to be deleted"))
            .until(namespaceNotFound(namespace));
      }
    } catch (Exception ex) {
      LoggerHelper.getLocal().warning(ex.getMessage());
      LoggerHelper.getLocal().warning("Cleanup failed");
    }
  }

  /**
   * Returns true if no artifacts exists in the given namespace.
   *
   * @param namespace name of the namespace
   * @return true if no artifacts exists, otherwise false
   */
  public static Callable<Boolean> nothingFoundInNamespace(String namespace) {
    return () -> {
      boolean nothingFound = true;
      LoggerHelper.getLocal().info("Checking for "
          + "domains, "
          + "replica sets, "
          + "jobs, "
          + "config maps, "
          + "secrets, "
          + "persistent volume claims, "
          + "persistent volumes, "
          + "deployments, "
          + "services, "
          + "service accounts, "
          + "ingresses "
          + "namespaced roles"
          + "namespaced rolebindings in namespace " + namespace + "\n");

      // Check if any domains exist
      try {
        if (Kubernetes.listDomains(namespace) != null) {
          LoggerHelper.getLocal().info("Domain still exists !!!");
          LoggerHelper.getLocal().info(dump(Kubernetes.listDomains(namespace)));
          nothingFound = false;
        }
      } catch (Exception ex) {
        LoggerHelper.getLocal().warning(ex.getMessage());
        LoggerHelper.getLocal().warning("Failed to list domains");
      }

      // Check if any replica sets exist
      try {
        if (!Kubernetes.listReplicaSets(namespace).getItems().isEmpty()) {
          LoggerHelper.getLocal().info("ReplicaSets still exists!!!");
          List<V1ReplicaSet> items = Kubernetes.listReplicaSets(namespace).getItems();
          for (var item : items) {
            LoggerHelper.getLocal().info(item.getMetadata().getName());
          }
          nothingFound = false;
        }
      } catch (Exception ex) {
        LoggerHelper.getLocal().warning(ex.getMessage());
        LoggerHelper.getLocal().warning("Failed to list replica sets");
      }

      // check if any jobs exist
      try {
        if (!Kubernetes.listJobs(namespace).getItems().isEmpty()) {
          LoggerHelper.getLocal().info("Jobs still exists!!!");
          List<V1Job> items = Kubernetes.listJobs(namespace).getItems();
          for (var item : items) {
            LoggerHelper.getLocal().info(item.getMetadata().getName());
          }
          nothingFound = false;
        }
      } catch (Exception ex) {
        LoggerHelper.getLocal().warning(ex.getMessage());
        LoggerHelper.getLocal().warning("Failed to list jobs");
      }

      // check if any configmaps exist
      try {
        if (!Kubernetes.listConfigMaps(namespace).getItems().isEmpty()) {
          LoggerHelper.getLocal().info("Config Maps still exists!!!");
          List<V1ConfigMap> items = Kubernetes.listConfigMaps(namespace).getItems();
          for (var item : items) {
            LoggerHelper.getLocal().info(item.getMetadata().getName());
          }
          nothingFound = false;
        }
      } catch (Exception ex) {
        LoggerHelper.getLocal().warning(ex.getMessage());
        LoggerHelper.getLocal().warning("Failed to list config maps");
      }

      // check if any secrets exist
      try {
        if (!Kubernetes.listSecrets(namespace).getItems().isEmpty()) {
          LoggerHelper.getLocal().info("Secrets still exists!!!");
          List<V1Secret> items = Kubernetes.listSecrets(namespace).getItems();
          for (var item : items) {
            LoggerHelper.getLocal().info(item.getMetadata().getName());
          }
          nothingFound = false;
        }
      } catch (Exception ex) {
        LoggerHelper.getLocal().warning(ex.getMessage());
        LoggerHelper.getLocal().warning("Failed to list secrets");
      }

      // check if any persistent volume claims exist
      try {
        if (!Kubernetes.listPersistentVolumeClaims(namespace).getItems().isEmpty()) {
          LoggerHelper.getLocal().info("Persistent Volumes Claims still exists!!!");
          List<V1PersistentVolumeClaim> items = Kubernetes.listPersistentVolumeClaims(namespace).getItems();
          for (var item : items) {
            LoggerHelper.getLocal().info(item.getMetadata().getName());
          }
          nothingFound = false;
        }
      } catch (Exception ex) {
        LoggerHelper.getLocal().warning(ex.getMessage());
        LoggerHelper.getLocal().warning("Failed to list persistent volume claims");
      }

      // check if any persistent volumes exist
      try {
        for (var item : Kubernetes.listPersistentVolumeClaims(namespace).getItems()) {
          String label = Optional.ofNullable(item)
              .map(pvc -> pvc.getMetadata())
              .map(metadata -> metadata.getLabels())
              .map(labels -> labels.get("weblogic.domainUid")).get();

          if (!Kubernetes.listPersistentVolumes(
              String.format("weblogic.domainUid = %s", label))
              .getItems().isEmpty()) {
            LoggerHelper.getLocal().info("Persistent Volumes still exists!!!");
            List<V1PersistentVolume> pvs = Kubernetes.listPersistentVolumes(
                String.format("weblogic.domainUid = %s", label))
                .getItems();
            for (var pv : pvs) {
              LoggerHelper.getLocal().info(pv.getMetadata().getName());
            }
            nothingFound = false;
          }
        }
      } catch (Exception ex) {
        LoggerHelper.getLocal().warning(ex.getMessage());
        LoggerHelper.getLocal().warning("Failed to list persistent volumes");
      }

      // check if any deployments exist
      try {
        if (!Kubernetes.listDeployments(namespace).getItems().isEmpty()) {
          LoggerHelper.getLocal().info("Deployments still exists!!!");
          List<V1Deployment> items = Kubernetes.listDeployments(namespace).getItems();
          for (var item : items) {
            LoggerHelper.getLocal().info(item.getMetadata().getName());
          }
          nothingFound = false;
        }
      } catch (Exception ex) {
        LoggerHelper.getLocal().warning(ex.getMessage());
        LoggerHelper.getLocal().warning("Failed to list deployments");
      }

      // check if any services exist
      try {
        if (!Kubernetes.listServices(namespace).getItems().isEmpty()) {
          LoggerHelper.getLocal().info("Services still exists!!!");
          List<V1Service> items = Kubernetes.listServices(namespace).getItems();
          for (var item : items) {
            LoggerHelper.getLocal().info(item.getMetadata().getName());
          }
          nothingFound = false;
        }
      } catch (Exception ex) {
        LoggerHelper.getLocal().warning(ex.getMessage());
        LoggerHelper.getLocal().warning("Failed to list services");
      }

      // check if any service accounts exist
      try {
        if (!Kubernetes.listServiceAccounts(namespace).getItems().isEmpty()) {
          LoggerHelper.getLocal().info("Service Accounts still exists!!!");
          List<V1ServiceAccount> items = Kubernetes.listServiceAccounts(namespace).getItems();
          for (var item : items) {
            LoggerHelper.getLocal().info(item.getMetadata().getName());
          }
          nothingFound = false;
        }
      } catch (Exception ex) {
        LoggerHelper.getLocal().warning(ex.getMessage());
        LoggerHelper.getLocal().warning("Failed to list service accounts");
      }

      // check if any ingress exist
      try {
        if (!Kubernetes.listNamespacedIngresses(namespace).getItems().isEmpty()) {
          LoggerHelper.getLocal().info("Ingresses still exists!!!");
          List<ExtensionsV1beta1Ingress> items = Kubernetes.listNamespacedIngresses(namespace).getItems();
          for (var item : items) {
            LoggerHelper.getLocal().info(item.getMetadata().getName());
          }
          nothingFound = false;
        }
      } catch (Exception ex) {
        LoggerHelper.getLocal().warning(ex.getMessage());
        LoggerHelper.getLocal().warning("Failed to list Ingresses");
      }

      // check if any namespaced roles exist
      try {
        if (!Kubernetes.listNamespacedRoles(namespace).getItems().isEmpty()) {
          LoggerHelper.getLocal().info("Namespaced roles still exists!!!");
          List<V1Role> items = Kubernetes.listNamespacedRoles(namespace).getItems();
          for (var item : items) {
            LoggerHelper.getLocal().info(item.getMetadata().getName());
          }
          nothingFound = false;
        }
      } catch (Exception ex) {
        LoggerHelper.getLocal().warning(ex.getMessage());
        LoggerHelper.getLocal().warning("Failed to list namespaced roles");
      }

      // check if any namespaced role bindings exist
      try {
        if (!Kubernetes.listNamespacedRoleBinding(namespace).getItems().isEmpty()) {
          LoggerHelper.getLocal().info("Namespaced role bindings still exists!!!");
          List<V1RoleBinding> items = Kubernetes.listNamespacedRoleBinding(namespace).getItems();
          for (var item : items) {
            LoggerHelper.getLocal().info(item.getMetadata().getName());
          }
          nothingFound = false;
        }
      } catch (Exception ex) {
        LoggerHelper.getLocal().warning(ex.getMessage());
        LoggerHelper.getLocal().warning("Failed to list namespaced role bindings");
      }

      return nothingFound;
    };

  }

  /**
   * Return true if the namespace was not found.
   *
   * @param namespace name of the namespace
   * @return true if namespace was not found, otherwise false
   */
  public static Callable<Boolean> namespaceNotFound(String namespace) {
    return () -> {
      boolean notFound = true;
      // get namespaces
      try {
        List<String> namespaceList = Kubernetes.listNamespaces();
        if (namespaceList.contains(namespace)) {
          LoggerHelper.getLocal().info("Namespace still exists!!!");
          LoggerHelper.getLocal().info(namespace);
          notFound = false;
        }
      } catch (Exception ex) {
        LoggerHelper.getLocal().warning(ex.getMessage());
        LoggerHelper.getLocal().warning("Failed to list namespaces");
      }
      return notFound;
    };
  }

  /**
   * Deletes artifacts in the Kubernetes cluster in the given namespace.
   *
   * @param namespace name of the namespace
   */
  public static void deleteNamespacedArtifacts(String namespace) {
    LoggerHelper.getLocal().info("Deleting artifacts in namespace " + namespace);

    // Delete deployments
    try {
      for (var item : Kubernetes.listDeployments(namespace).getItems()) {
        Kubernetes.deleteDeployment(namespace, item.getMetadata().getName());
      }
    } catch (Exception ex) {
      LoggerHelper.getLocal().warning(ex.getMessage());
      LoggerHelper.getLocal().warning("Failed to delete deployments");
    }

    // Delete replicasets
    try {
      for (var item : Kubernetes.listReplicaSets(namespace).getItems()) {
        Kubernetes.deleteReplicaSet(namespace, item.getMetadata().getName());
      }
    } catch (Exception ex) {
      LoggerHelper.getLocal().warning(ex.getMessage());
      LoggerHelper.getLocal().warning("Failed to delete replica sets");
    }

    // Delete jobs
    try {
      for (var item : Kubernetes.listJobs(namespace).getItems()) {
        Kubernetes.deleteJob(namespace, item.getMetadata().getName());
      }
    } catch (Exception ex) {
      LoggerHelper.getLocal().warning(ex.getMessage());
      LoggerHelper.getLocal().warning("Failed to delete jobs");
    }

    // Delete configmaps
    try {
      for (var item : Kubernetes.listConfigMaps(namespace).getItems()) {
        Kubernetes.deleteConfigMap(item.getMetadata().getName(), namespace);
      }
    } catch (Exception ex) {
      LoggerHelper.getLocal().warning(ex.getMessage());
      LoggerHelper.getLocal().warning("Failed to delete config maps");
    }

    // Delete secrets
    try {
      for (var item : Kubernetes.listSecrets(namespace).getItems()) {
        Kubernetes.deleteSecret(item.getMetadata().getName(), namespace);
      }
    } catch (Exception ex) {
      LoggerHelper.getLocal().warning(ex.getMessage());
      LoggerHelper.getLocal().warning("Failed to delete secrets");
    }

    // Delete pv
    try {
      for (var item : Kubernetes.listPersistentVolumeClaims(namespace).getItems()) {
        String label = Optional.ofNullable(item)
            .map(pvc -> pvc.getMetadata())
            .map(metadata -> metadata.getLabels())
            .map(labels -> labels.get("weblogic.domainUid")).get();
        for (var pv : Kubernetes.listPersistentVolumes(
            String.format("weblogic.domainUid = %s", label)).getItems()) {
          Kubernetes.deletePv(pv.getMetadata().getName());
        }
      }
    } catch (ApiException ex) {
      LoggerHelper.getLocal().warning(ex.getResponseBody());
    } catch (Exception ex) {
      LoggerHelper.getLocal().warning(ex.getMessage());
      LoggerHelper.getLocal().warning("Failed to delete persistent volumes");
    }

    // Delete pvc
    try {
      for (var item : Kubernetes.listPersistentVolumeClaims(namespace).getItems()) {
        Kubernetes.deletePvc(item.getMetadata().getName(), namespace);
      }
    } catch (Exception ex) {
      LoggerHelper.getLocal().warning(ex.getMessage());
      LoggerHelper.getLocal().warning("Failed to delete persistent volume claims");
    }

    // Delete services
    try {
      for (var item : Kubernetes.listServices(namespace).getItems()) {
        Kubernetes.deleteService(item.getMetadata().getName(), namespace);
      }
    } catch (Exception ex) {
      LoggerHelper.getLocal().warning(ex.getMessage());
      LoggerHelper.getLocal().warning("Failed to delete services");
    }

    // Delete namespaced roles
    try {
      for (var item : Kubernetes.listNamespacedRoles(namespace).getItems()) {
        Kubernetes.deleteNamespacedRole(namespace, item.getMetadata().getName());
      }
    } catch (Exception ex) {
      LoggerHelper.getLocal().warning(ex.getMessage());
      LoggerHelper.getLocal().warning("Failed to delete namespaced roles");
    }

    // Delete namespaced role bindings
    try {
      for (var item : Kubernetes.listNamespacedRoleBinding(namespace).getItems()) {
        Kubernetes.deleteNamespacedRoleBinding(namespace, item.getMetadata().getName());
      }
    } catch (Exception ex) {
      LoggerHelper.getLocal().warning(ex.getMessage());
      LoggerHelper.getLocal().warning("Failed to delete namespaced rolebindings");
    }

    // Delete service accounts
    try {
      for (var item : Kubernetes.listServiceAccounts(namespace).getItems()) {
        Kubernetes.deleteServiceAccount(item.getMetadata().getName(), namespace);
      }
    } catch (Exception ex) {
      LoggerHelper.getLocal().warning(ex.getMessage());
      LoggerHelper.getLocal().warning("Failed to delete service accounts");
    }

  }

  /**
   * Delete a namespace.
   *
   * @param namespace name of the namespace
   */
  public static void deleteNamespace(String namespace) {
    try {
      Kubernetes.deleteNamespace(namespace);
    } catch (Exception ex) {
      LoggerHelper.getLocal().warning(ex.getMessage());
      LoggerHelper.getLocal().warning("Failed to delete namespace");
    }
  }

}
