// Copyright 2019, Oracle Corporation and/or its affiliates. All rights reserverd.

package oracle.kubernetes.clustermanager.types;

import java.util.Date;

/** Represents a lease on a Compute Instance. */
public class Lease {

  public String shape;
  public Date expiryTime;
}
