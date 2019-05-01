// Copyright 2019, Oracle Corporation and/or its affiliates. All rights reserved.

package oracle.kubernetes.clustermanager.helpers;

import com.google.common.base.Strings;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.core.ComputeClient;
import com.oracle.bmc.core.ComputeWaiters;
import com.oracle.bmc.core.VirtualNetworkClient;
import com.oracle.bmc.core.model.*;
import com.oracle.bmc.core.requests.*;
import com.oracle.bmc.core.responses.*;
import com.oracle.bmc.identity.Identity;
import com.oracle.bmc.identity.IdentityClient;
import com.oracle.bmc.identity.model.AvailabilityDomain;
import com.oracle.bmc.identity.requests.ListAvailabilityDomainsRequest;
import com.oracle.bmc.identity.responses.ListAvailabilityDomainsResponse;
import java.io.File;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import oracle.kubernetes.clustermanager.types.ClusterSpec;
import oracle.kubernetes.clustermanager.types.Lease;
import org.apache.commons.io.FileUtils;

/** Create and destroy Kubernetes clusters. */
public class ClusterHelper {

  /** Create a new cluster. */
  public static void createCluster(
      AuthenticationDetailsProvider provider, Lease lease, ClusterSpec spec) {

    String instanceName = "ephemeral-kubernetes-" + lease.getId();
    String compartmentId = System.getenv("CLUSTER_MANAGER_COMPARTMENT_OCID");
    String vcnId = System.getenv("CLUSTER_MANAGER_VCN_OCID");
    String sshPublicKey = System.getenv("CLUSTER_MANAGER_SSH_PUBLIC_KEY");
    String subnetName = "Public Subnet " + lease.getRegion();
    String imageName = "Oracle-Linux-7.6-2019.01.17-0";

    ComputeClient computeClient = new ComputeClient(provider);
    computeClient.setRegion(Region.US_PHOENIX_1);
    VirtualNetworkClient vcnClient = new VirtualNetworkClient(provider);

    // get the availability domain object
    List<AvailabilityDomain> availabilityDomains = null;
    try {
      availabilityDomains = getAvailabilityDomains(provider, compartmentId);
    } catch (Exception e) {
      System.err.println("could not get availability domains");
      System.exit(1);
    }
    AvailabilityDomain adToUse = null;
    for (AvailabilityDomain item : availabilityDomains) {
      if (item.getName().equals(lease.getRegion())) {
        adToUse = item;
        break;
      }
    }
    if (adToUse == null) {
      System.err.println("did not find AD " + lease.getRegion());
      System.exit(1);
    }

    // get the subnet object
    List<Subnet> subnets = null;
    try {
      subnets = getSubnets(vcnClient, compartmentId, vcnId);
    } catch (Exception e) {
      System.err.println("could not get subnets");
      System.exit(1);
    }
    Subnet subnet = null;
    for (Subnet item : subnets) {
      if (item.getDisplayName().equals(subnetName)) {
        subnet = item;
        break;
      }
    }
    if (subnet == null) {
      System.err.println("did not find subnet " + subnetName);
      System.exit(1);
    }

    // get the image object
    List<Image> images = null;
    try {
      images = getImages(provider, computeClient, compartmentId);
    } catch (Exception e) {
      System.err.println("could not get images");
      System.exit(1);
    }
    Image image = null;
    for (Image item : images) {
      if (item.getDisplayName().equals(imageName)) {
        image = item;
        break;
      }
    }
    if (image == null) {
      System.err.println("could not find image " + imageName);
      System.exit(1);
    }

    // get the shape object
    List<Shape> shapes = null;
    try {
      shapes = getShapes(computeClient, compartmentId);
    } catch (Exception e) {
      System.err.println("could not get shapes");
      System.exit(1);
    }
    Shape shape = null;
    for (Shape item : shapes) {
      if (item.getShape().equals(lease.getShape())) {
        shape = item;
        break;
      }
    }
    if (shape == null) {
      System.err.println("could not find shape " + lease.getShape());
      System.exit(1);
    }

    Instance instance =
        createInstance(
            computeClient,
            compartmentId,
            adToUse,
            instanceName,
            image,
            shape,
            subnet,
            sshPublicKey,
            null); // kmsKeyId

    System.out.println("Instance is being created...");

    Instance theInstance = null;
    try {
      theInstance = waitForInstanceProvisioningToComplete(computeClient, instance.getId());
    } catch (Exception e) {
      System.err.println("got exception while waiting for image to be created");
      System.exit(1);
    }
    if (theInstance == null) {
      System.err.println("instance was null - this should not happen");
      System.exit(1);
    }

    // need to get the kubeconfig now




    System.out.println("Instance is provisioned.");
  }

  /** Destroy a cluster. */
  public static void destroyCluster(Lease lease) {}

  private static List<Image> getImages(
      AuthenticationDetailsProvider provider, ComputeClient computeClient, String compartmentId)
      throws Exception {

    ListImagesResponse response =
        computeClient.listImages(ListImagesRequest.builder().compartmentId(compartmentId).build());

    return response.getItems();
  }

  private static List<AvailabilityDomain> getAvailabilityDomains(
      AuthenticationDetailsProvider provider, String compartmentId) throws Exception {

    Identity identityClient = new IdentityClient(provider);
    identityClient.setRegion(Region.US_PHOENIX_1);

    ListAvailabilityDomainsResponse listAvailabilityDomainsResponse =
        identityClient.listAvailabilityDomains(
            ListAvailabilityDomainsRequest.builder().compartmentId(compartmentId).build());

    identityClient.close();

    return listAvailabilityDomainsResponse.getItems();
  }

  private static List<Shape> getShapes(ComputeClient computeClient, String compartmentId) {

    ListShapesResponse response =
        computeClient.listShapes(ListShapesRequest.builder().compartmentId(compartmentId).build());

    return response.getItems();
  }

  private static List<Subnet> getSubnets(VirtualNetworkClient vcnClient, String compartmentId, String vcnId) {

    ListSubnetsResponse response =
        vcnClient.listSubnets(ListSubnetsRequest.builder().vcnId(vcnId).compartmentId(compartmentId).build());

    return response.getItems();
  }

  private static Instance createInstance(
      ComputeClient computeClient,
      String compartmentId,
      AvailabilityDomain availabilityDomain,
      String instanceName,
      Image image,
      Shape shape,
      Subnet subnet,
      String sshPublicKey,
      String kmsKeyId) {

    Map<String, String> metadata = new HashMap<>();
    metadata.put("ssh_authorized_keys", sshPublicKey);

    String encodedString = null;
    try {
      // TODO pick the right file
      byte[] fileContent =
          FileUtils.readFileToByteArray(new File("src/main/cloud-init/olcs-1.9.sh"));
      encodedString = Base64.getEncoder().encodeToString(fileContent);
    } catch (Exception e) {
      System.err.println("could not base64 encode the cloud-init script");
      System.exit(1);
    }

    metadata.put("user_data", encodedString);

    InstanceSourceViaImageDetails details =
        (Strings.isNullOrEmpty(kmsKeyId))
            ? InstanceSourceViaImageDetails.builder().imageId(image.getId()).build()
            : InstanceSourceViaImageDetails.builder()
                .imageId(image.getId())
                .kmsKeyId(kmsKeyId)
                .build();

    LaunchInstanceResponse response =
        computeClient.launchInstance(
            LaunchInstanceRequest.builder()
                .launchInstanceDetails(
                    LaunchInstanceDetails.builder()
                        .availabilityDomain(availabilityDomain.getName())
                        .compartmentId(compartmentId)
                        .displayName(instanceName)
                        .faultDomain("FAULT-DOMAIN-1") // optional parameter
                        .metadata(metadata)
                        .shape(shape.getShape())
                        .sourceDetails(details)
                        .createVnicDetails(
                            CreateVnicDetails.builder().subnetId(subnet.getId()).build())
                        .build())
                .build());

    return response.getInstance();
  }

  private static Instance waitForInstanceProvisioningToComplete(
      ComputeClient computeClient, String instanceId) throws Exception {

    ComputeWaiters waiter = computeClient.getWaiters();
    GetInstanceResponse response =
        waiter
            .forInstance(
                GetInstanceRequest.builder().instanceId(instanceId).build(),
                Instance.LifecycleState.Running)
            .execute();

    return response.getInstance();
  }
}
