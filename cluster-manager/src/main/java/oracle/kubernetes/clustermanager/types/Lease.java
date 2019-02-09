// Copyright 2019, Oracle Corporation and/or its affiliates. All rights reserved.

package oracle.kubernetes.clustermanager.types;

import java.util.Date;

/** Represents a lease on a Compute Instance. */
public class Lease {

  public int id;
  public String shape;
  public String region;
  public String tenant;
  public int buildNumber;
  public Date expiryTime;

  public Lease(
      int id, String shape, String region, String tenant, int buildNumber, Date expiryTime) {
    this.id = id;
    this.shape = shape;
    this.region = region;
    this.tenant = tenant;
    this.buildNumber = buildNumber;
    this.expiryTime = expiryTime;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getShape() {
    return shape;
  }

  public void setShape(String shape) {
    this.shape = shape;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public String getTenant() {
    return tenant;
  }

  public void setTenant(String tenant) {
    this.tenant = tenant;
  }

  public int getBuildNumber() {
    return buildNumber;
  }

  public void setBuildNumber(int buildNumber) {
    this.buildNumber = buildNumber;
  }

  public Date getExpiryTime() {
    return expiryTime;
  }

  public void setExpiryTime(Date expiryTime) {
    this.expiryTime = expiryTime;
  }

  @Override
  public String toString() {
    return "Lease{"
        + "id="
        + id
        + ", shape='"
        + shape
        + '\''
        + ", region='"
        + region
        + '\''
        + ", tenant='"
        + tenant
        + '\''
        + ", buildNumber="
        + buildNumber
        + ", expiryTime="
        + expiryTime
        + '}';
  }
}
