// Copyright (c) 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package oracle.kubernetes.operator.utils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;

import io.kubernetes.client.Copy;
import io.kubernetes.client.extended.generic.GenericKubernetesApi;
import io.kubernetes.client.extended.generic.KubernetesApiResponse;
import io.kubernetes.client.extended.generic.options.DeleteOptions;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.openapi.apis.ExtensionsV1beta1Api;
import io.kubernetes.client.openapi.apis.RbacAuthorizationV1Api;
import io.kubernetes.client.openapi.models.ExtensionsV1beta1Ingress;
import io.kubernetes.client.openapi.models.ExtensionsV1beta1IngressList;
import io.kubernetes.client.openapi.models.V1ClusterRoleBinding;
import io.kubernetes.client.openapi.models.V1ClusterRoleBindingList;
import io.kubernetes.client.openapi.models.V1ClusterRoleList;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ConfigMapList;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.openapi.models.V1Event;
import io.kubernetes.client.openapi.models.V1EventList;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobList;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1NamespaceBuilder;
import io.kubernetes.client.openapi.models.V1NamespaceList;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1ObjectMetaBuilder;
import io.kubernetes.client.openapi.models.V1PersistentVolume;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaim;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaimList;
import io.kubernetes.client.openapi.models.V1PersistentVolumeList;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodCondition;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1ReplicaSet;
import io.kubernetes.client.openapi.models.V1ReplicaSetList;
import io.kubernetes.client.openapi.models.V1RoleBindingList;
import io.kubernetes.client.openapi.models.V1RoleList;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1SecretList;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceAccount;
import io.kubernetes.client.openapi.models.V1ServiceAccountList;
import io.kubernetes.client.openapi.models.V1ServiceList;
import io.kubernetes.client.util.ClientBuilder;
import org.awaitility.core.ConditionFactory;
import org.glassfish.hk2.utilities.reflection.Logger;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.with;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

// TODO ryan - in here we want to implement all of the kubernetes
// primitives that we need, using the API, not spawning a process
// to run kubectl.
public class Kubernetes {

  private static String PRETTY = "false";
  private static Boolean ALLOW_WATCH_BOOKMARKS = false;
  private static String RESOURCE_VERSION = "";
  private static Integer TIMEOUT_SECONDS = 5;
  private static String DOMAIN_GROUP = "weblogic.oracle";
  private static String DOMAIN_VERSION = "v7";
  private static String DOMAIN_PLURAL = "domains";
  private static String FOREGROUND = "Foreground";
  private static String BACKGROUND = "Background";
  private static int GRACE_PERIOD = 0;
  private static Logger logger;

  // Core Kubernetes API clients
  private static ApiClient apiClient = null;
  private static CoreV1Api coreV1Api = null;
  private static CustomObjectsApi customObjectsApi = null;
  private static RbacAuthorizationV1Api rbacAuthApi = null;
  private static DeleteOptions deleteOptions = null;

  // Extended GenericKubernetesApi clients
  private static GenericKubernetesApi<V1ConfigMap, V1ConfigMapList> configMapClient = null;
  private static GenericKubernetesApi<V1ClusterRoleBinding, V1ClusterRoleBindingList> roleBindingClient = null;
  private static GenericKubernetesApi<V1Deployment, V1DeploymentList> deploymentClient = null;
  private static GenericKubernetesApi<V1Job, V1JobList> jobClient = null;
  private static GenericKubernetesApi<V1Namespace, V1NamespaceList> namespaceClient = null;
  private static GenericKubernetesApi<V1Pod, V1PodList> podClient = null;
  private static GenericKubernetesApi<V1PersistentVolume, V1PersistentVolumeList> pvClient = null;
  private static GenericKubernetesApi<V1PersistentVolumeClaim, V1PersistentVolumeClaimList> pvcClient = null;
  private static GenericKubernetesApi<V1ReplicaSet, V1ReplicaSetList> rsClient = null;
  private static GenericKubernetesApi<V1Secret, V1SecretList> secretClient = null;
  private static GenericKubernetesApi<V1Service, V1ServiceList> serviceClient = null;
  private static GenericKubernetesApi<V1ServiceAccount, V1ServiceAccountList> serviceAccountClient = null;

  private static ConditionFactory withStandardRetryPolicy = null;

  static {
    try {
      Configuration.setDefaultApiClient(ClientBuilder.defaultClient());
      apiClient = Configuration.getDefaultApiClient();
      coreV1Api = new CoreV1Api();
      customObjectsApi = new CustomObjectsApi();
      rbacAuthApi = new RbacAuthorizationV1Api();
      initializeGenericKubernetesApiClients();
      // create standard, reusable retry/backoff policy
      withStandardRetryPolicy = with().pollDelay(2, SECONDS)
          .and().with().pollInterval(10, SECONDS)
          .atMost(5, MINUTES).await();
    } catch (IOException ioex) {
      throw new ExceptionInInitializerError(ioex);
    }
  }

  /**
   * Create static instances of GenericKubernetesApi clients.
   */
  private static void initializeGenericKubernetesApiClients() {
    // Invocation parameters aren't changing so create them as statics
    configMapClient =
        new GenericKubernetesApi<>(
            V1ConfigMap.class,  // the api type class
            V1ConfigMapList.class, // the api list type class
            "", // the api group
            "v1", // the api version
            "configmaps", // the resource plural
            apiClient //the api client
        );

    deploymentClient =
        new GenericKubernetesApi<>(
            V1Deployment.class,  // the api type class
            V1DeploymentList.class, // the api list type class
            "", // the api group
            "v1", // the api version
            "deployments", // the resource plural
            apiClient //the api client
        );

    jobClient =
        new GenericKubernetesApi<>(
            V1Job.class,  // the api type class
            V1JobList.class, // the api list type class
            "", // the api group
            "v1", // the api version
            "jobs", // the resource plural
            apiClient //the api client
        );

    namespaceClient =
        new GenericKubernetesApi<>(
            V1Namespace.class, // the api type class
            V1NamespaceList.class, // the api list type class
            "", // the api group
            "v1", // the api version
            "namespaces", // the resource plural
            apiClient //the api client
        );

    podClient =
        new GenericKubernetesApi<>(
            V1Pod.class,  // the api type class
            V1PodList.class, // the api list type class
            "", // the api group
            "v1", // the api version
            "pods", // the resource plural
            apiClient //the api client
        );

    pvClient =
        new GenericKubernetesApi<>(
            V1PersistentVolume.class,  // the api type class
            V1PersistentVolumeList.class, // the api list type class
            "", // the api group
            "v1", // the api version
            "persistentvolumes", // the resource plural
            apiClient //the api client
        );

    pvcClient =
        new GenericKubernetesApi<>(
            V1PersistentVolumeClaim.class,  // the api type class
            V1PersistentVolumeClaimList.class, // the api list type class
            "", // the api group
            "v1", // the api version
            "persistentvolumeclaims", // the resource plural
            apiClient //the api client
        );

    rsClient =
        new GenericKubernetesApi<>(
            V1ReplicaSet.class, // the api type class
            V1ReplicaSetList.class, // the api list type class
            "", // the api group
            "v1", // the api version
            "replicasets", // the resource plural
            apiClient //the api client
        );

    roleBindingClient =
        new GenericKubernetesApi<>(
            V1ClusterRoleBinding.class, // the api type class
            V1ClusterRoleBindingList.class, // the api list type class
            "rbac.authorization.k8s.io", // the api group
            "v1", // the api version
            "clusterrolebindings", // the resource plural
            apiClient //the api client
        );

    secretClient =
        new GenericKubernetesApi<>(
            V1Secret.class,  // the api type class
            V1SecretList.class, // the api list type class
            "", // the api group
            "v1", // the api version
            "secrets", // the resource plural
            apiClient //the api client
        );

    serviceClient =
        new GenericKubernetesApi<>(
            V1Service.class,  // the api type class
            V1ServiceList.class, // the api list type class
            "", // the api group
            "v1", // the api version
            "services", // the resource plural
            apiClient //the api client
        );

    serviceAccountClient =
        new GenericKubernetesApi<>(
            V1ServiceAccount.class,  // the api type class
            V1ServiceAccountList.class, // the api list type class
            "", // the api group
            "v1", // the api version
            "serviceaccounts", // the resource plural
            apiClient //the api client
        );
    deleteOptions = new DeleteOptions();
    deleteOptions.setGracePeriodSeconds(0L);
    deleteOptions.setPropagationPolicy(FOREGROUND);
  }

  // ------------------------  deployments -----------------------------------

  /**
   * List deployments in the given namespace.
   *
   * @param namespace namespace in which to list the deployments
   * @return list of deployment objects as {@link V1DeploymentList}
   * @throws ApiException when listing fails
   */
  public static V1DeploymentList listDeployments(String namespace) throws ApiException {
    V1DeploymentList deployments;
    try {
      AppsV1Api apiInstance = new AppsV1Api(apiClient);
      deployments = apiInstance.listNamespacedDeployment(
          namespace, // String | namespace.
          PRETTY, // String | If 'true', then the output is pretty printed.
          ALLOW_WATCH_BOOKMARKS, // Boolean | allowWatchBookmarks requests watch events with type "BOOKMARK".
          null, // String | The continue option should be set when retrieving more results from the server.
          null, // String | A selector to restrict the list of returned objects by their fields.
          null, // String | A selector to restrict the list of returned objects by their labels.
          null, // Integer | limit is a maximum number of responses to return for a list call.
          RESOURCE_VERSION, // String | Shows changes that occur after that particular version of a resource.
          TIMEOUT_SECONDS, // Integer | Timeout for the list call.
          Boolean.FALSE // Boolean | Watch for changes to the described resources.
      );
    } catch (ApiException apex) {
      LoggerHelper.getLocal().log(Level.WARNING, apex.getResponseBody());
      throw apex;
    }
    return deployments;
  }

  // --------------------------- pods -----------------------------------------
  /**
   * Get a pod's log.
   *
   * @param name name of the Pod
   * @param namespace name of the Namespace
   * @return log as a String
   * @throws ApiException if Kubernetes client API call fails
   */
  public static String getPodLog(String name, String namespace) throws ApiException {
    return getPodLog(name, namespace, null);
  }

  /**
   * Get a pod's log.
   *
   * @param name name of the Pod
   * @param namespace name of the Namespace
   * @param container name of container for which to stream logs
   * @return log as a String
   * @throws ApiException if Kubernetes client API call fails
   */
  public static String getPodLog(String name, String namespace, String container)
      throws ApiException {
    String log = null;
    try {
      log = coreV1Api.readNamespacedPodLog(
          name, // name of the Pod
          namespace, // name of the Namespace
          container, // container for which to stream logs
          null, //  Boolean Follow the log stream of the pod
          null, // number of bytes to read from the server before terminating the log output
          PRETTY, // pretty print output
          null, // Boolean, Return previous terminated container logs
          null, // relative time (seconds) before the current time from which to show logs
          null, // number of lines from the end of the logs to show
          null // Boolean, add timestamp at the beginning of every line of log output
      );
    } catch (ApiException apex) {
      LoggerHelper.getLocal().log(Level.SEVERE, apex.getResponseBody());
      throw apex;
    }

    return log;
  }

  /**
   * Create a pod.
   *
   * @param namespace name of the namespace
   * @param podBody V1Pod object containing pod configuration data
   * @return V1Pod object
   * @throws ApiException when create pod fails
   */
  public static V1Pod createPod(String namespace, V1Pod podBody) throws ApiException {
    V1Pod pod;
    try {
      pod = coreV1Api.createNamespacedPod(namespace, podBody, null, null, null);
    } catch (ApiException apex) {
      LoggerHelper.getLocal().log(Level.SEVERE, apex.getResponseBody());
      throw apex;
    }
    return pod;
  }

  /**
   * Delete a Kubernetes Pod.
   *
   * @param name name of the pod
   * @param namespace name of namespace
   * @return true if successful
   */
  public static boolean deletePod(String name, String namespace) {

    KubernetesApiResponse<V1Pod> response = podClient.delete(namespace, name);

    if (!response.isSuccess()) {
      LoggerHelper.getLocal().log(Level.WARNING, "Failed to delete pod '" + name + "' from namespace: "
          + namespace + " with HTTP status code: " + response.getHttpStatusCode());
      return false;
    }

    if (response.getObject() != null) {
      LoggerHelper.getLocal().log(Level.INFO,
          "Received after-deletion status of the requested object, will be deleting "
              + "pod in background!");
    }

    return true;
  }

  /**
   * List all pods in given namespace.
   *
   * @param namespace Namespace in which to list all pods
   * @param labelSelectors with which the pods are decorated
   * @return V1PodList list of pods
   * @throws ApiException when there is error in querying the cluster
   */
  public static V1PodList listPods(String namespace, String labelSelectors) throws ApiException {
    V1PodList v1PodList = null;
    try {
      v1PodList
          = coreV1Api.listNamespacedPod(
              namespace, // namespace in which to look for the pods.
              Boolean.FALSE.toString(), // pretty print output.
              Boolean.FALSE, // allowWatchBookmarks requests watch events with type "BOOKMARK".
              null, // continue to query when there is more results to return.
              null, // selector to restrict the list of returned objects by their fields
              labelSelectors, // selector to restrict the list of returned objects by their labels.
              null, // maximum number of responses to return for a list call.
              null, // shows changes that occur after that particular version of a resource.
              null, // Timeout for the list/watch call.
              Boolean.FALSE // Watch for changes to the described resources.
          );
    } catch (ApiException apex) {
      LoggerHelper.getLocal().log(Level.SEVERE, apex.getResponseBody());
      throw apex;
    }
    return v1PodList;
  }

  /**
   * Returns the V1Pod object given the following parameters.
   * @param namespace in which to check for the pod existence
   * @param labelSelector in the format "weblogic.domainUID in (%s)"
   * @param podName name of the pod to return
   * @return V1Pod object if found otherwise null
   * @throws ApiException when there is error in querying the cluster
   */
  public static V1Pod getPod(String namespace, String labelSelector, String podName) throws ApiException {
    V1PodList v1PodList =
        coreV1Api.listNamespacedPod(
            namespace, // namespace in which to look for the pods.
            Boolean.FALSE.toString(), // // pretty print output.
            Boolean.FALSE, // allowWatchBookmarks requests watch events with type "BOOKMARK".
            null, // continue to query when there is more results to return.
            null, // selector to restrict the list of returned objects by their fields
            labelSelector, // selector to restrict the list of returned objects by their labels.
            null, // maximum number of responses to return for a list call.
            null, // shows changes that occur after that particular version of a resource.
            null, // Timeout for the list/watch call.
            Boolean.FALSE // Watch for changes to the described resources.
        );
    for (V1Pod item : v1PodList.getItems()) {
      if (item.getMetadata().getName().startsWith(podName.trim())) {
        LoggerHelper.getLocal().log(Level.INFO, "Pod Name: " + item.getMetadata().getName());
        LoggerHelper.getLocal().log(Level.INFO, "Pod Namespace: " + item.getMetadata().getNamespace());
        LoggerHelper.getLocal().log(Level.INFO, "Pod UID: " + item.getMetadata().getUid());
        LoggerHelper.getLocal().log(Level.INFO, "Pod Status: " + item.getStatus().getPhase());
        return item;
      }
    }
    return null;
  }

  /**
   * Checks if a pod is ready in a given namespace.
   *
   * @param namespace in which to check if the pod is ready
   * @param labelSelector the labels the pd is decorated with
   * @param podName name of the pod to check for
   * @return true if the pod is in the ready condition, false otherwise
   * @throws ApiException if Kubernetes client API call fails
   */
  public static Callable<Boolean> podReady(String namespace, String labelSelector, String podName)
      throws ApiException {
    return () -> {
      boolean status = false;
      V1Pod pod = getPod(namespace, labelSelector, podName);
      if (pod != null) {
        // get the podCondition with the 'Ready' type field
        V1PodCondition v1PodReadyCondition = pod.getStatus().getConditions().stream()
            .filter(v1PodCondition -> "Ready".equals(v1PodCondition.getType()))
            .findAny()
            .orElse(null);
        if (v1PodReadyCondition != null) {
          status = v1PodReadyCondition.getStatus().equalsIgnoreCase("true");
        }
      } else {
        LoggerHelper.getLocal().log(Level.WARNING, "Pod doesn't exist");
      }
      return status;
    };
  }

  /**
   * Checks if a pod is ready in a given namespace.
   *
   * @param namespace in which to check if the pod is ready
   * @param labelSelector the labels the pd is decorated with
   * @param podName name of the pod to check for
   * @return true if the pod is found, false otherwise
   * @throws ApiException if Kubernetes client API call fails
   */
  public static Callable<Boolean> podDoesNotExist(String namespace, String labelSelector, String podName)
      throws ApiException {
    return () -> {
      V1Pod pod = getPod(namespace, labelSelector, podName);
      return pod == null;
    };
  }


  /**
   * Copy a directory from Kubernetes pod to local destination path.
   * @param pod V1Pod object
   * @param srcPath source directory location
   * @param destination destination directory path
   * @throws IOException when copy fails
   * @throws ApiException when pod interaction fails
   */
  public static void copyDirectoryFromPod(V1Pod pod, String srcPath, Path destination)
      throws IOException, ApiException {
    Copy copy = new Copy();
    copy.copyDirectoryFromPod(pod, srcPath, destination);
  }

  // --------------------------- namespaces -----------------------------------
  /**
   * Create a Kubernetes namespace.
   *
   * @param name the name of the namespace
   * @return true on success, false otherwise
   * @throws ApiException if Kubernetes client API call fails
   */
  public static boolean createNamespace(String name) throws ApiException {
    V1ObjectMeta meta = new V1ObjectMetaBuilder().withName(name).build();
    V1Namespace namespace = new V1NamespaceBuilder().withMetadata(meta).build();

    try {
      coreV1Api.createNamespace(
          namespace, // name of the Namespace
          PRETTY, // pretty print output
          null, // indicates that modifications should not be persisted
          null // name associated with the actor or entity that is making these changes
      );
    } catch (ApiException apex) {
      LoggerHelper.getLocal().log(Level.SEVERE, apex.getResponseBody());
      throw apex;
    }

    return true;
  }

  /**
   * Create a Kubernetes namespace.
   *
   * @param namespace - V1Namespace object containing namespace configuration data
   * @return true if successful
   * @throws ApiException if Kubernetes client API call fails
   */
  public static boolean createNamespace(V1Namespace namespace) throws ApiException {
    if (namespace == null) {
      throw new IllegalArgumentException(
          "Parameter 'namespace' cannot be null when calling createNamespace()");
    }

    V1Namespace ns = null;
    try {
      ns = coreV1Api.createNamespace(
          namespace, // V1Namespace configuration data object
          PRETTY, // pretty print output
          null, // indicates that modifications should not be persisted
          null // name associated with the actor or entity that is making these changes
      );
    } catch (ApiException apex) {
      LoggerHelper.getLocal().log(Level.SEVERE, apex.getResponseBody());
      throw apex;
    }

    return true;
  }

  /**
   * List namespaces in the Kubernetes cluster.
   * @return List of all Namespace names in the Kubernetes cluster
   * @throws ApiException if Kubernetes client API call fails
   */
  public static List<String> listNamespaces() throws ApiException {
    ArrayList<String> nameSpaces = new ArrayList<>();
    V1NamespaceList namespaceList;
    try {
      namespaceList = coreV1Api.listNamespace(
          PRETTY, // pretty print output
          ALLOW_WATCH_BOOKMARKS, // allowWatchBookmarks requests watch events with type "BOOKMARK"
          null, // set when retrieving more results from the server
          null, // selector to restrict the list of returned objects by their fields
          null, // selector to restrict the list of returned objects by their labels
          null, // maximum number of responses to return for a list call
          RESOURCE_VERSION, // shows changes that occur after that particular version of a resource
          TIMEOUT_SECONDS, // Timeout for the list/watch call
          false // Watch for changes to the described resources
      );
    } catch (ApiException apex) {
      LoggerHelper.getLocal().log(Level.SEVERE, apex.getResponseBody());
      throw apex;
    }

    for (V1Namespace namespace : namespaceList.getItems()) {
      nameSpaces.add(namespace.getMetadata().getName());
    }

    return nameSpaces;
  }

  /**
   * List namespaces in the Kubernetes cluster as V1NamespaceList.
   * @return V1NamespaceList of Namespace in the Kubernetes cluster
   * @throws ApiException if Kubernetes client API call fails
   */
  public static V1NamespaceList listNamespacesAsObjects() throws ApiException {
    V1NamespaceList namespaceList;
    try {
      namespaceList = coreV1Api.listNamespace(
          PRETTY, // pretty print output
          ALLOW_WATCH_BOOKMARKS, // allowWatchBookmarks requests watch events with type "BOOKMARK"
          null, // set when retrieving more results from the server
          null, // selector to restrict the list of returned objects by their fields
          null, // selector to restrict the list of returned objects by their labels
          null, // maximum number of responses to return for a list call
          RESOURCE_VERSION, // shows changes that occur after that particular version of a resource
          TIMEOUT_SECONDS, // Timeout for the list/watch call
          false // Watch for changes to the described resources
      );
    } catch (ApiException apex) {
      LoggerHelper.getLocal().log(Level.SEVERE, apex.getResponseBody());
      throw apex;
    }

    return namespaceList;
  }


  /**
   * Delete a namespace for the given name.
   *
   * @param name name of namespace
   * @return true if successful delete request, false otherwise.
   */
  public static boolean deleteNamespace(String name) {

    KubernetesApiResponse<V1Namespace> response = namespaceClient.delete(name);

    if (!response.isSuccess()) {
      // status 409 means contents in the namespace being removed,
      // once done namespace will be purged
      if (response.getHttpStatusCode() == 409) {
        LoggerHelper.getLocal().log(Level.WARNING, response.getStatus().getMessage());
        return false;
      } else {
        LoggerHelper.getLocal().log(Level.WARNING, "Failed to delete namespace: "
            + name + " with HTTP status code: " + response.getHttpStatusCode());
        return false;
      }
    }

    withStandardRetryPolicy.conditionEvaluationListener(
        condition -> LoggerHelper.getLocal().log(Level.INFO,
          "Waiting for namespace " + name + " to be deleted "
            + "(elapsed time " + condition.getElapsedTimeInMS() + "ms, "
            + "remaining time " + condition.getRemainingTimeInMS() + "ms)"))
              .until(assertDoesNotThrow(() -> namespaceDeleted(name),
                  String.format("namespaceExists failed with ApiException for namespace %s",
                  name)));

    return true;
  }

  private static Callable<Boolean> namespaceDeleted(String namespace) throws ApiException {
    return new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        List<String> namespaces = listNamespaces();
        if (!namespaces.contains(namespace)) {
          return true;
        }
        return  false;
      }
    };
  }

  // --------------------------- Events ---------------------------------------------------

  /**
   * List events in a namespace.
   *
   * @param namespace name of the namespace in which to list events
   * @return List of {@link V1Event} objects
   * @throws ApiException when listing events fails
   */
  public static List<V1Event> listNamespacedEvents(String namespace) throws ApiException {
    List<V1Event> events = null;
    try {
      V1EventList list = coreV1Api.listNamespacedEvent(
          namespace, // String | namespace.
          PRETTY, // String | If 'true', then the output is pretty printed.
          ALLOW_WATCH_BOOKMARKS, // Boolean | allowWatchBookmarks requests watch events with type "BOOKMARK".
          null, // String | The continue option should be set when retrieving more results from the server.
          null, // String | A selector to restrict the list of returned objects by their fields.
          null, // String | A selector to restrict the list of returned objects by their labels.
          null, // Integer | limit is a maximum number of responses to return for a list call.
          RESOURCE_VERSION, // String | Shows changes that occur after that particular version of a resource.
          TIMEOUT_SECONDS, // Integer | Timeout for the list call.
          Boolean.FALSE // Boolean | Watch for changes to the described resources.
      );
      events = list.getItems();
      events.sort(Comparator.comparing(e -> e.getMetadata().getCreationTimestamp()));
      Collections.reverse(events);
    } catch (ApiException apex) {
      LoggerHelper.getLocal().log(Level.WARNING, apex.getResponseBody());
      throw apex;
    }
    return events;
  }

  // --------------------------- Custom Resource Domain -----------------------------------

  /**
   * List Domain Custom Resources for a given namespace.
   *
   * @param namespace name of namespace
   * @return List of Domain Custom Resources
   */
  public static Object listDomains(String namespace) throws ApiException {
    Object domains = customObjectsApi.listNamespacedCustomObject(
        DOMAIN_GROUP,
        DOMAIN_VERSION,
        namespace,
        DOMAIN_PLURAL,
        PRETTY,
        null,
        null,
        null,
        null,
        RESOURCE_VERSION,
        TIMEOUT_SECONDS,
        null
    );
    return domains;
  }

  // --------------------------- config map ---------------------------

  /**
   * List Config Maps in the Kubernetes cluster.
   *
   * @param namespace Namespace in which to query
   * @return V1ConfigMapList of Config Maps
   * @throws ApiException if Kubernetes client API call fails
   */
  public static V1ConfigMapList listConfigMaps(String namespace) throws ApiException {

    V1ConfigMapList configMapList;
    try {
      configMapList = coreV1Api.listNamespacedConfigMap(
          namespace, // config map's namespace
          PRETTY, // pretty print output
          ALLOW_WATCH_BOOKMARKS, // allowWatchBookmarks requests watch events with type "BOOKMARK"
          null, // set when retrieving more results from the server
          null, // selector to restrict the list of returned objects by their fields
          null, // selector to restrict the list of returned objects by their labels
          null, // maximum number of responses to return for a list call
          RESOURCE_VERSION, // shows changes that occur after that particular version of a resource
          TIMEOUT_SECONDS, // Timeout for the list/watch call
          false // Watch for changes to the described resources
      );
    } catch (ApiException apex) {
      LoggerHelper.getLocal().log(Level.SEVERE, apex.getResponseBody());
      throw apex;
    }

    return configMapList;
  }

  // --------------------------- secret ---------------------------
  /**
   * List secrets in the Kubernetes cluster.
   * @param namespace Namespace in which to query
   * @return V1SecretList of secrets in the Kubernetes cluster
   */
  public static V1SecretList listSecrets(String namespace) {
    KubernetesApiResponse<V1SecretList> list = secretClient.list(namespace);
    if (list.isSuccess()) {
      return list.getObject();
    } else {
      LoggerHelper.getLocal().log(Level.WARNING, "Failed to list secrets, status code {0}", list.getHttpStatusCode());
      return null;
    }
  }

  // --------------------------- pv/pvc ---------------------------

  /**
   * List all persistent volumes in the Kubernetes cluster.
   * @return V1PersistentVolumeList of Persistent Volumes in Kubernetes cluster
   */
  public static V1PersistentVolumeList listPersistentVolumes() {
    KubernetesApiResponse<V1PersistentVolumeList> list = pvClient.list();
    if (list.isSuccess()) {
      return list.getObject();
    } else {
      LoggerHelper.getLocal().log(Level.WARNING, "Failed to list Persistent Volumes,"
          + " status code {0}", list.getHttpStatusCode());
      return null;
    }
  }

  /**
   * List persistent volumes in the Kubernetes cluster based on the label.
   * @param labels String containing the labels the PV is decorated with
   * @return V1PersistentVolumeList list of Persistent Volumes
   * @throws ApiException when listing fails
   */
  public static V1PersistentVolumeList listPersistentVolumes(String labels) throws ApiException {
    V1PersistentVolumeList listPersistentVolume;
    try {
      listPersistentVolume = coreV1Api.listPersistentVolume(
          PRETTY, // pretty print output
          ALLOW_WATCH_BOOKMARKS, // allowWatchBookmarks requests watch events with type "BOOKMARK"
          null, // set when retrieving more results from the server
          null, // selector to restrict the list of returned objects by their fields
          labels, // selector to restrict the list of returned objects by their labels
          null, // maximum number of responses to return for a list call
          RESOURCE_VERSION, // shows changes that occur after that particular version of a resource
          TIMEOUT_SECONDS, // Timeout for the list/watch call
          false // Watch for changes to the described resources
      );
    } catch (ApiException apex) {
      LoggerHelper.getLocal().log(Level.SEVERE, apex.getResponseBody());
      throw apex;
    }
    return listPersistentVolume;
  }

  /**
   * List persistent volume claims in the namespace.
   * @param namespace name of the namespace in which to list
   * @return V1PersistentVolumeClaimList of Persistent Volume Claims in namespace
   */
  public static V1PersistentVolumeClaimList listPersistentVolumeClaims(String namespace) {
    KubernetesApiResponse<V1PersistentVolumeClaimList> list = pvcClient.list(namespace);
    if (list.isSuccess()) {
      return list.getObject();
    } else {
      LoggerHelper.getLocal().log(Level.WARNING, "Failed to list Persistent Volumes claims,"
          + " status code {0}", list.getHttpStatusCode());
      return null;
    }
  }

  // --------------------------- service account ---------------------------
  /**
   * List all service accounts in the Kubernetes cluster.
   *
   * @param namespace Namespace in which to list all service accounts
   * @return V1ServiceAccountList of service accounts
   */
  public static V1ServiceAccountList listServiceAccounts(String namespace) {
    KubernetesApiResponse<V1ServiceAccountList> list = serviceAccountClient.list(namespace);
    if (list.isSuccess()) {
      return list.getObject();
    } else {
      LoggerHelper.getLocal().log(Level.WARNING, "Failed to list service accounts, status code {0}",
          list.getHttpStatusCode());
      return null;
    }
  }
  // --------------------------- Services ---------------------------
  
  /**
   * List services in a given namespace.
   *
   * @param namespace name of the namespace
   * @return V1ServiceList list of {@link V1Service} objects
   */
  public static V1ServiceList listServices(String namespace) {

    KubernetesApiResponse<V1ServiceList> list = serviceClient.list(namespace);
    if (list.isSuccess()) {
      return list.getObject();
    } else {
      LoggerHelper.getLocal().log(Level.WARNING, "Failed to list services in namespace");
      return null;
    }
  }

  // --------------------------- jobs ---------------------------
  /**
   * List jobs in the given namespace.
   *
   * @param namespace in which to list the jobs
   * @return V1JobList list of {@link V1Job} from Kubernetes cluster
   * @throws ApiException when list fails
   */
  public static V1JobList listJobs(String namespace) throws ApiException {
    V1JobList list;
    try {
      BatchV1Api apiInstance = new BatchV1Api(apiClient);
      list = apiInstance.listNamespacedJob(
          namespace, // String | name of the namespace.
          PRETTY, // String | pretty print output.
          ALLOW_WATCH_BOOKMARKS, // Boolean | allowWatchBookmarks requests watch events with type "BOOKMARK".
          null, // String | The continue option should be set when retrieving more results from the server.
          null, // String | A selector to restrict the list of returned objects by their fields.
          null, // String | A selector to restrict the list of returned objects by their labels.
          null, // Integer | limit is a maximum number of responses to return for a list call.
          RESOURCE_VERSION, // String | Shows changes that occur after that particular version of a resource.
          TIMEOUT_SECONDS, // Integer | Timeout for the list/watch call.
          Boolean.FALSE // Boolean | Watch for changes to the described resources
      );
    } catch (ApiException apex) {
      LoggerHelper.getLocal().log(Level.WARNING, apex.getResponseBody());
      throw apex;
    }
    return list;
  }

  // --------------------------- replica sets ---------------------------
  /**
   * List replica sets in the given namespace.
   *
   * @param namespace in which to list the replica sets
   * @return V1ReplicaSetList list of {@link V1ReplicaSet} objects
   * @throws ApiException when list fails
   */
  public static V1ReplicaSetList listReplicaSets(String namespace) throws ApiException {
    try {
      AppsV1Api apiInstance = new AppsV1Api(apiClient);
      V1ReplicaSetList list = apiInstance.listNamespacedReplicaSet(
          namespace, // String | namespace.
          PRETTY, // String | If 'true', then the output is pretty printed.
          ALLOW_WATCH_BOOKMARKS, // Boolean | allowWatchBookmarks requests watch events with type "BOOKMARK".
          null, // String | The continue option should be set when retrieving more results from the server.
          null, // String | A selector to restrict the list of returned objects by their fields.
          null, // String | A selector to restrict the list of returned objects by their labels.
          null, // Integer | limit is a maximum number of responses to return for a list call.
          RESOURCE_VERSION, // String | Shows changes that occur after that particular version of a resource.
          TIMEOUT_SECONDS, // Integer | Timeout for the list call.
          Boolean.FALSE // Boolean | Watch for changes to the described resources.
      );
      return list;
    } catch (ApiException apex) {
      LoggerHelper.getLocal().log(Level.WARNING, apex.getResponseBody());
      throw apex;
    }
  }

  // --------------------------- Role-based access control (RBAC)   ---------------------------
  /**
   * List cluster role bindings.
   *
   * @param labelSelector labels to narrow the list
   * @return V1RoleBindingList list of {@link V1RoleBinding} objects
   * @throws ApiException when listing fails
   */
  public static V1RoleBindingList listClusterRoleBindings(String labelSelector) throws ApiException {
    V1RoleBindingList roleBindings;
    try {
      roleBindings = rbacAuthApi.listRoleBindingForAllNamespaces(
          ALLOW_WATCH_BOOKMARKS, // Boolean | allowWatchBookmarks requests watch events with type "BOOKMARK".
          null, // String | The continue option should be set when retrieving more results from the server.
          null, // String | A selector to restrict the list of returned objects by their fields.
          labelSelector, // String | A selector to restrict the list of returned objects by their labels.
          null, // Integer | limit is a maximum number of responses to return for a list call.
          PRETTY, // String | If true, then the output is pretty printed.
          RESOURCE_VERSION, // String | Shows changes that occur after that particular version of a resource.
          TIMEOUT_SECONDS, // Integer | Timeout for the list/watch call.
          Boolean.FALSE // Boolean | Watch for changes to the described resources
      );
    } catch (ApiException apex) {
      LoggerHelper.getLocal().log(Level.WARNING, apex.getResponseBody());
      throw apex;
    }
    return roleBindings;
  }

  /**
   * List role bindings in a given namespace.
   *
   * @param namespace name of the namespace
   * @return V1RoleBindingList list of {@link V1RoleBinding} objects
   * @throws ApiException when listing fails
   */
  public static V1RoleBindingList listNamespacedRoleBinding(String namespace)
      throws ApiException {
    V1RoleBindingList roleBindings;
    try {
      roleBindings = rbacAuthApi.listNamespacedRoleBinding(
          namespace, // String | namespace.
          PRETTY, // String | If 'true', then the output is pretty printed.
          ALLOW_WATCH_BOOKMARKS, // Boolean | allowWatchBookmarks requests watch events with type "BOOKMARK".
          null, // String | The continue option should be set when retrieving more results from the server.
          null, // String | A selector to restrict the list of returned objects by their fields.
          null, // String | A selector to restrict the list of returned objects by their labels.
          null, // Integer | limit is a maximum number of responses to return for a list call.
          RESOURCE_VERSION, // String | Shows changes that occur after that particular version of a resource.
          TIMEOUT_SECONDS, // Integer | Timeout for the list call.
          Boolean.FALSE // Boolean | Watch for changes to the described resources.
      );
    } catch (ApiException apex) {
      LoggerHelper.getLocal().log(Level.WARNING, apex.getResponseBody());
      throw apex;
    }
    return roleBindings;
  }



  /**
   * List cluster roles in the Kubernetes cluster.
   *
   * @param labelSelector labels to narrow the list
   * @return V1ClusterRoleList list of {@link V1ClusterRole} objects
   * @throws ApiException when listing fails
   */
  public static V1ClusterRoleList listClusterRoles(String labelSelector) throws ApiException {
    V1ClusterRoleList roles;
    try {
      roles = rbacAuthApi.listClusterRole(
          PRETTY, // String | If 'true', then the output is pretty printed.
          ALLOW_WATCH_BOOKMARKS, // Boolean | allowWatchBookmarks requests watch events with type "BOOKMARK".
          null, // String | The continue option should be set when retrieving more results from the server.
          null, // String | A selector to restrict the list of returned objects by their fields.
          labelSelector, // String | A selector to restrict the list of returned objects by their labels.
          null, // Integer | limit is a maximum number of responses to return for a list call.
          RESOURCE_VERSION, // String | Shows changes that occur after that particular version of a resource.
          TIMEOUT_SECONDS, // Integer | Timeout for the list call.
          Boolean.FALSE // Boolean | Watch for changes to the described resources.
      );
    } catch (ApiException apex) {
      LoggerHelper.getLocal().log(Level.WARNING, apex.getResponseBody());
      throw apex;
    }
    return roles;
  }



  /**
   * List roles in a given namespace.
   *
   * @param namespace name of the namespace
   * @return V1RoleList list of {@link V1Role} object
   * @throws ApiException when listing fails
   */
  public static V1RoleList listNamespacedRoles(String namespace) throws ApiException {
    V1RoleList roles;
    try {
      roles = rbacAuthApi.listNamespacedRole(
          namespace, // String | namespace.
          PRETTY, // String | If 'true', then the output is pretty printed.
          ALLOW_WATCH_BOOKMARKS, // Boolean | allowWatchBookmarks requests watch events with type "BOOKMARK".
          null, // String | The continue option should be set when retrieving more results from the server.
          null, // String | A selector to restrict the list of returned objects by their fields.
          null, // String | A selector to restrict the list of returned objects by their labels.
          null, // Integer | limit is a maximum number of responses to return for a list call.
          RESOURCE_VERSION, // String | Shows changes that occur after that particular version of a resource.
          TIMEOUT_SECONDS, // Integer | Timeout for the list call.
          Boolean.FALSE // Boolean | Watch for changes to the described resources.
      );
    } catch (ApiException apex) {
      LoggerHelper.getLocal().log(Level.WARNING, apex.getResponseBody());
      throw apex;
    }
    return roles;
  }

  /**
   * List Ingresses in the given namespace.
   *
   * @param namespace name of the namespace
   * @return ExtensionsV1beta1IngressList list of {@link ExtensionsV1beta1Ingress} objects
   * @throws ApiException when listing fails
   */
  public static ExtensionsV1beta1IngressList listNamespacedIngresses(String namespace) throws ApiException {
    ExtensionsV1beta1IngressList ingressList;
    try {
      ExtensionsV1beta1Api apiInstance = new ExtensionsV1beta1Api(apiClient);
      ingressList = apiInstance.listNamespacedIngress(
          namespace, // namespace
          PRETTY, // String | If 'true', then the output is pretty printed.
          ALLOW_WATCH_BOOKMARKS, // Boolean | allowWatchBookmarks requests watch events with type "BOOKMARK".
          null, // String | The continue option should be set when retrieving more results from the server.
          null, // String | A selector to restrict the list of returned objects by their fields.
          null, // String | A selector to restrict the list of returned objects by their labels.
          null, // Integer | limit is a maximum number of responses to return for a list call.
          RESOURCE_VERSION, // String | Shows changes that occur after that particular version of a resource.
          TIMEOUT_SECONDS, // Integer | Timeout for the list/watch call.
          ALLOW_WATCH_BOOKMARKS // Boolean | Watch for changes to the described resources.
      );
    } catch (ApiException apex) {
      LoggerHelper.getLocal().log(Level.WARNING, apex.getResponseBody());
      throw apex;
    }
    return ingressList;
  }

  /**
   * Get Ingress in the given namespace by name.
   *
   * @param namespace name of the namespace
   * @param name name of the Ingress object
   * @return ExtensionsV1beta1Ingress Ingress object when found, otherwise null
   * @throws ApiException when get fails
   */
  public static ExtensionsV1beta1Ingress getNamespacedIngress(String namespace, String name)
      throws ApiException {
    try {
      for (ExtensionsV1beta1Ingress item
          : listNamespacedIngresses(namespace).getItems()) {
        if (name.equals(item.getMetadata().getName())) {
          return item;
        }
      }
    } catch (ApiException apex) {
      LoggerHelper.getLocal().log(Level.WARNING, apex.getResponseBody());
      throw apex;
    }
    return null;
  }

}
