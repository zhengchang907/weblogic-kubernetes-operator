// Copyright (c) 2018, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package oracle.kubernetes.weblogic.domain.model;

import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import oracle.kubernetes.json.Description;
import oracle.kubernetes.json.EnumClass;
import oracle.kubernetes.json.Range;
import oracle.kubernetes.operator.ServerStartPolicy;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * An element representing a cluster in the domain configuration.
 *
 * @since 2.0
 */
@Description("An element representing a cluster in the domain configuration.")
public class Cluster extends BaseConfiguration implements Comparable<Cluster> {
  /** The name of the cluster. Required. */
  @Description("The name of this cluster. Required")
  @Nonnull
  private String clusterName;

  /** The number of replicas to run in the cluster, if specified. */
  @Description("The number of cluster members to run.")
  @Range(minimum = 0)
  private Integer replicas;

  /**
   * Tells the operator whether the customer wants the server to be running. For clustered servers -
   * the operator will start it if the policy is ALWAYS or the policy is IF_NEEDED and the server
   * needs to be started to get to the cluster's replica count.
   *
   * @since 2.0
   */
  @EnumClass(value = ServerStartPolicy.class, qualifier = "forCluster")
  @Description(
      "The strategy for deciding whether to start a server. "
          + "Legal values are NEVER, or IF_NEEDED.")
  private String serverStartPolicy;

  @Description(
      "The maximum number of cluster members that can be temporarily unavailable. Defaults to 1.")
  @Range(minimum = 1)
  private Integer maxUnavailable;

  @Description("Customization affecting ClusterIP Kubernetes services for the WebLogic cluster.")
  @SerializedName("clusterService")
  @Expose
  private KubernetesResource clusterService = new KubernetesResource();

  @Description("If true (the default), then the number of replicas is allowed to drop below the "
      + "minimum dynamic cluster size configured in the WebLogic domain home configuration. "
      + "Otherwise, the operator will ensure that the number of replicas is not less than "
      + "the minimum dynamic cluster setting. This setting applies to dynamic clusters only."
  )
  private Boolean allowReplicasBelowMinDynClusterSize;

  @Description("If true (the default), the operator may start up more than one managed server "
       + "in this cluster at the same time during scale up operations. Otherwise, the operator "
       + "will wait until a managed server to be in Ready state before starting up the next "
       + "one when scaling up more than one server."
  )
  private Boolean allowConcurrentScaleUp;

  protected Cluster getConfiguration() {
    Cluster configuration = new Cluster();
    configuration.fillInFrom(this);
    configuration.setRestartVersion(this.getRestartVersion());
    return configuration;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(@Nonnull String clusterName) {
    this.clusterName = clusterName;
  }

  Cluster withClusterName(@Nonnull String clusterName) {
    setClusterName(clusterName);
    return this;
  }

  public Integer getReplicas() {
    return replicas;
  }

  public void setReplicas(Integer replicas) {
    this.replicas = replicas;
  }

  /**
   * Whether to allow number of replicas to drop below the minimum dynamic cluster size configured
   * in the WebLogic domain home configuration.
   *
   * @return whether to allow number of replicas to drop below the minimum dynamic cluster size
   *     configured in the WebLogic domain home configuration.
   */
  public Boolean isAllowReplicasBelowMinDynClusterSize() {
    return allowReplicasBelowMinDynClusterSize;
  }

  public void setAllowReplicasBelowMinDynClusterSize(Boolean value) {
    allowReplicasBelowMinDynClusterSize = value;
  }

  /**
   * Whether to allow the operator to start more than one managed servers at * the same
   *     time during scale up operation.
   *
   * @return whether to allow the operator to start more than one managed servers at * the same
   *     time during scale up operation.
   */
  public Boolean isAllowConcurrentScaleUp() {
    return allowConcurrentScaleUp;
  }

  public void setAllowConcurrentScaleUp(Boolean value) {
    allowConcurrentScaleUp = value;
  }

  @Nullable
  @Override
  public String getServerStartPolicy() {
    return serverStartPolicy;
  }

  @Override
  public void setServerStartPolicy(String serverStartPolicy) {
    this.serverStartPolicy = serverStartPolicy;
  }

  public KubernetesResource getClusterService() {
    return clusterService;
  }

  public void setClusterService(KubernetesResource clusterService) {
    this.clusterService = clusterService;
  }

  public Map<String, String> getClusterLabels() {
    return clusterService.getLabels();
  }

  void addClusterLabel(String name, String value) {
    clusterService.addLabel(name, value);
  }

  public Map<String, String> getClusterAnnotations() {
    return clusterService.getAnnotations();
  }

  void addClusterAnnotation(String name, String value) {
    clusterService.addAnnotations(name, value);
  }

  Integer getMaxUnavailable() {
    return maxUnavailable;
  }

  void setMaxUnavailable(Integer maxUnavailable) {
    this.maxUnavailable = maxUnavailable;
  }

  void fillInFrom(Cluster other) {
    if (other == null) {
      return;
    }
    super.fillInFrom(other);
    clusterService.fillInFrom(other.clusterService);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .appendSuper(super.toString())
        .append("clusterName", clusterName)
        .append("replicas", replicas)
        .append("serverStartPolicy", serverStartPolicy)
        .append("clusterService", clusterService)
        .append("maxUnavailable", maxUnavailable)
        .append("allowReplicasBelowMinDynClusterSize", allowReplicasBelowMinDynClusterSize)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Cluster cluster = (Cluster) o;

    return new EqualsBuilder()
        .appendSuper(super.equals(o))
        .append(clusterName, cluster.clusterName)
        .append(replicas, cluster.replicas)
        .append(serverStartPolicy, cluster.serverStartPolicy)
        .append(clusterService, cluster.clusterService)
        .append(maxUnavailable, cluster.maxUnavailable)
        .append(allowReplicasBelowMinDynClusterSize, cluster.allowReplicasBelowMinDynClusterSize)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .appendSuper(super.hashCode())
        .append(clusterName)
        .append(replicas)
        .append(serverStartPolicy)
        .append(clusterService)
        .append(maxUnavailable)
        .append(allowReplicasBelowMinDynClusterSize)
        .toHashCode();
  }

  @Override
  public int compareTo(@Nonnull Cluster o) {
    return clusterName.compareTo(o.clusterName);
  }
}
