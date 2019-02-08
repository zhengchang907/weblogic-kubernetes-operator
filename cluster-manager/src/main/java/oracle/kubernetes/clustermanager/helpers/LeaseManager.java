// Copyright 2019, Oracle Corporation and/or its affiliates. All rights reserved.

package oracle.kubernetes.clustermanager.helpers;

import java.util.ArrayList;
import java.util.List;
import oracle.kubernetes.clustermanager.types.Lease;

/** Manages leases on Compute Instances. */
public class LeaseManager {

  /** Attempt to acquire a lease. */
  public void acquireLease() {}

  /** Get a list of current leases. */
  public List<Lease> getLeases() {

    return new ArrayList<>();
  }
}
