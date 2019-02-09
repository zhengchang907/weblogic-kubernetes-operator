// Copyright 2019, Oracle Corporation and/or its affiliates. All rights reserved.

package oracle.kubernetes.clustermanager.helpers;

import static oracle.kubernetes.clustermanager.Main.*;

import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import com.oracle.bmc.objectstorage.responses.PutObjectResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import oracle.kubernetes.clustermanager.types.Lease;
import oracle.kubernetes.clustermanager.types.Leases;
import org.apache.commons.io.IOUtils;

/** Manages leases on Compute Instances. */
public class LeaseManager {

  /** Attempt to acquire a lease. */
  public static void acquireLease(ObjectStorage client, String tenant, int buildNumber) {
    Leases leases = getLeases(client);

    // find a vacancy
    main_loop:
    for (Lease lease : leases.getLeases()) {
      if (lease.getTenant() == null) {
        System.out.println("found a vacancy in unit " + lease.getId());
        lease.setTenant(tenant);
        lease.setBuildNumber(buildNumber);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, 3);
        lease.setExpiryTime(cal.getTime());
        break main_loop;
      }
    }

    for (Lease theLease : leases.getLeases()) {
      System.out.println(theLease);
    }

    // update the leases
    PutObjectRequest putObjectRequest =
        PutObjectRequest.builder()
            .namespaceName(NAMESPACE)
            .bucketName(BUCKET_NAME)
            .objectName(OBJECT_NAME)
            .ifMatch(leases.geteTag())
            .putObjectBody(
                new ByteArrayInputStream(leases.toContent().getBytes(StandardCharsets.UTF_8)))
            .build();
    PutObjectResponse response = client.putObject(putObjectRequest);

    if (response.getETag().equals(leases.geteTag())) {
      System.out.println("Same etag...");
    } else {
      System.out.println("Different etag " + response.getETag());
    }
  }

  /** Get a list of current leases. */
  public static Leases getLeases(ObjectStorage client) {
    GetObjectRequest getObjectRequest =
        GetObjectRequest.builder()
            .namespaceName(NAMESPACE)
            .bucketName(BUCKET_NAME)
            .objectName(OBJECT_NAME)
            .build();
    GetObjectResponse response = client.getObject(getObjectRequest);
    System.out.println("Got the lease object with e-tag: " + response.getETag());

    String content = null;
    try {
      content = IOUtils.toString(response.getInputStream(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      System.err.println("could not read the data");
      System.exit(1);
    }

    Leases result = new Leases();
    List<Lease> leases = new ArrayList<>();

    for (String line : content.split("\n")) {
      String[] parts = line.split(",", -1);
      // limit -1 prevents split() from throwing away empty strings
      leases.add(
          new Lease(
              Integer.parseInt(parts[0]), // id
              parts[1], // shape
              parts[2], // region
              (parts[3].isEmpty() ? null : parts[3]), // tenant
              (parts[4].isEmpty() ? 0 : Integer.parseInt(parts[4])), // build number
              (parts[5].isEmpty() ? null : new Date(parts[5])) // expiry time
              ));
    }

    result.seteTag(response.getETag());
    result.setLeases(leases);
    return result;
  }
}
