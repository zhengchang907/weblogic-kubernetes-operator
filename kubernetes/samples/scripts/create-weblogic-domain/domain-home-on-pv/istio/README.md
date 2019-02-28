# Basic features of WebLogic operator on istio

## Prerequisites

Suppose you have successfully create a domain on pv on WebLogic operator+istio. 

Make sure you can access istio ingressgateway. Follow [istio Control Ingress Traffic](https://istio.io/docs/tasks/traffic-management/ingress/) to determin `INGRESS_HOST` and `INGRESS_PORT`, then run:

```
$ export GATEWAY_URL=$INGRESS_HOST:$INGRESS_PORT
```

## Access testwebapp through istio ingressgateway

### Create a gateway and a virtualservice

Create a gateway and a virtualservice to route traffic from istio ingressgateway to domain1-cluster-cluster-1.default.svc.cluster.local:

```
$ kubectl create -f gw.yaml
```

### Deploy testwebapp to weblogic cluster

Deploy [testwebapp](../../../charts/application/testwebapp.war) using Weblogic Admin Console.

### Access testwebapp through istio ingressgateway

If you access http://$GATEWAY_URL/testwebapp/ using browser, you can see the following response:

```
InetAddress: domain1-managed-server1/10.244.0.22
InetAddress.hostname: domain1-managed-server1
```

You can also run the following command to send more requests:

```
$ sh run.sh 
sending 30 requests to slc09xwj.us.oracle.com:31380/testwebapp/ ...
 
the access count of pod domain1-managed-server1 is 14
the access count of pod domain1-managed-server2 is 16
```
