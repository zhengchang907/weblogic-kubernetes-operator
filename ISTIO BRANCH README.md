# Istio Branch

This branch contains changes made to the WebLogic Operator to get it to the point where it was able to successfully start a domain in an Istio environment.

## Building the Operator

The changes being made are not compatible with the unit tests.  Use the following command to build without running tests:

`mvn clean install -Dmaven.test.skip=true`

## Testing Setup
The general flow of steps for testing are:

* Install Istio
* Create namespaces for weblogic-operator and the domain
* Enable auto-injection of sidecar each of the namespaces
    * For example: `kubectl label namespace domain1 istio-injection=enabled`
* Install operator
* The operator pod with start with two containers named `weblogic-operator` and `istio-proxy`
* Build domain image
* Create domain by applying domain.yaml file
* Admin server and managed servers should each start with two containers named `weblogic-server` and `istio-proxy`

## Summary of Changes

The table below summarizes changes made so far within this branch and an explanation of why the change was required.

| Change Made | Reason |
| --- | --- |
| Disabled auto-injection for Introspection job | When the domain introspection job completes, the associated istio container within the job pod does not complete.  So the job never shows as completed and eventuall reaches a `BackoffLimitExceeded` state. |
| Change node manager listen address to 0.0.0.0 | Node manager is listening on sample-domain1-admin-server:5556 and wlst is using sample-domain1-admin-server:5556 to connect it.  istio-proxy intercepts all request to sample-domain1-admin-server:5556 and translate the request to 127.0.0.1:5556, even the downstream and upstream processes are located in same local container. |
| Add port 5556 to admin server service | Expose port 5556 in for istio-proxy | 
