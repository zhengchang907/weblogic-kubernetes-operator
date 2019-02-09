package oracle.kubernetes.clustermanager.types;

/** Describes the desired type of cluster. */
public class ClusterSpec {

  public enum version {
    K8S_1_13,
    K8S_1_12,
    K8S_1_11;
  }

  public enum type {
    OL_CONTAINER_SERVICES,
    VANILLA_KUBERNETES;
  }
}
