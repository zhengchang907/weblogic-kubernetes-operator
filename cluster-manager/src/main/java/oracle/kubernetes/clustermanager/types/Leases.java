// Copyright 2019, Oracle Corporation and/or its affiliates. All rights reserved.

package oracle.kubernetes.clustermanager.types;

import java.util.Date;
import java.util.List;

public class Leases {

  public String eTag;
  public List<Lease> leases;

  public String geteTag() {
    return eTag;
  }

  public void seteTag(String eTag) {
    this.eTag = eTag;
  }

  public List<Lease> getLeases() {
    return leases;
  }

  public void setLeases(List<Lease> leases) {
    this.leases = leases;
  }

  public String toContent() {
    StringBuffer buffer = new StringBuffer();
    for (Lease lease : leases) {
      buffer.append(lease.getId());
      buffer.append(",");
      buffer.append(notNull(lease.getShape()));
      buffer.append(",");
      buffer.append(notNull(lease.getRegion()));
      buffer.append(",");
      buffer.append(notNull(lease.getTenant()));
      buffer.append(",");
      buffer.append(lease.getBuildNumber());
      buffer.append(",");
      buffer.append(notNull(lease.getExpiryTime()));
      buffer.append("\n");
    }
    return buffer.toString();
  }

  private String notNull(String in) {
    return (in == null ? "" : in);
  }

  private String notNull(Date in) {
    return (in == null ? "" : in.toString());
  }
}
