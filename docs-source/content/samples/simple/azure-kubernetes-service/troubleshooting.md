---
title: "Troubleshooting"
date: 2020-11-24T18:22:31-05:00
weight: 3
description: "Troubleshooting."
---


- [Access Administration Console](#fail-to-access-administration-console): possible causes for Administration Console inaccessibility.
- [Domain debugging](#domain-debugging)
- [Pod Error](#get-pod-error-details): how to get details of the pod error.
- [WebLogic Image Tool failure](#weblogic-image-tool-failure)
- [WebLogic Kubernetes Operator installation failure](#weblogic-kubernetes-operator-installation-failure)   
   - [System pods are pending](#the-aks-cluster-system-pods-are-pending)
   - [WebLogic Kubernetes Operator ErrImagePull](#fail-to-pull-the-operator-image)
- [WSL2 bad timestamp](#wsl2-bad-timestamp)

#### Get pod error details

You may get the following message while creating the WebLogic domain: "the job status is not Completed!"

```text
status on iteration 20 of 20
pod domain1-create-weblogic-sample-domain-job-nj7wl status is Init:0/1
The create domain job is not showing status completed after waiting 300 seconds.
Check the log output for errors.
Error from server (BadRequest): container "create-weblogic-sample-domain-job" in pod "domain1-create-weblogic-sample-domain-job-nj7wl" is waiting to start: PodInitializing
[ERROR] Exiting due to failure - the job status is not Completed!
```

You can get further error details by running `kubectl describe pod`, as shown here:

```bash
$ kubectl describe pod <your-pod-name>
```

This is an output example:

```bash
$ kubectl describe pod domain1-create-weblogic-sample-domain-job-nj7wl
Events:
Type     Reason       Age                  From                                        Message
----     ------       ----                 ----                                        -------
Normal   Scheduled    4m2s                 default-scheduler                           Successfully assigned default/domain1-create-weblogic-sample-domain-job-qqv6k to aks-nodepool1-58449474-vmss000001
Warning  FailedMount  119s                 kubelet, aks-nodepool1-58449474-vmss000001  Unable to mount volumes for pod "domain1-create-weblogic-sample-domain-job-qqv6k_default(15706980-73cb-11ea-b804-b2c91b494b00)": timeout expired waiting for volumes to attach or mount for pod "default"/"domain1-create-weblogic-sample-domain-job-qqv6k". list of unmounted volumes=[weblogic-sample-domain-storage-volume]. list of unattached volumes=[create-weblogic-sample-domain-job-cm-volume weblogic-sample-domain-storage-volume weblogic-credentials-volume default-token-zr7bq]
Warning  FailedMount  114s (x9 over 4m2s)  kubelet, aks-nodepool1-58449474-vmss000001  MountVolume.SetUp failed for volume "wls-azurefile" : Couldn't get secret default/azure-secrea
```

#### Fail to access Administration Console

Here are some common reasons for this failure, along with some tips to help you investigate.

* **Create WebLogic domain job fails**

Check the deploy log and find the failure details with `kubectl describe pod podname`.
Please go to 1. Getting pod error details.

* **Process of starting the servers is still running**

Check with `kubectl get svc` and if domainUID-admin-server, domainUID-managed-server1 and domainUID-managed-server2 are not listed,
we need to wait some more for the Administration Server to start.

The following output is an example of when the Administration Server has started.

```bash
$ kubectl get svc
NAME                               TYPE           CLUSTER-IP    EXTERNAL-IP     PORT(S)              AGE
domain1-admin-server               ClusterIP      None          <none>          30012/TCP,7001/TCP   7m3s
domain1-admin-server-ext           NodePort       10.0.78.211   <none>          7001:30701/TCP       7m3s
domain1-admin-server-external-lb   LoadBalancer   10.0.6.144    40.71.233.81    7001:32758/TCP       7m32s
domain1-cluster-1-lb               LoadBalancer   10.0.29.231   52.142.39.152   8001:31022/TCP       7m30s
domain1-cluster-cluster-1          ClusterIP      10.0.80.134   <none>          8001/TCP             1s
domain1-managed-server1            ClusterIP      None          <none>          8001/TCP             1s
domain1-managed-server2            ClusterIP      None          <none>          8001/TCP             1s
internal-weblogic-operator-svc     ClusterIP      10.0.1.23     <none>          8082/TCP             9m59s
kubernetes                         ClusterIP      10.0.0.1      <none>          443/TCP              16m
```

If services are up but the WLS Administration Console is still not available, use `kubectl describe domain` to check domain status.

```bash
$ kubectl describe domain domain1
```

Make sure the status of cluster-1 is `ServersReady` and `Available`. The status of admin-server, managed-server1 and managed-server2 should be `RUNNING`. Otherwise, the cluster is likely still in the process of becoming fully ready.

{{%expand "Click here to view the example status." %}}
```yaml
Status:
   Clusters:
   Cluster Name:      cluster-1
   Maximum Replicas:  5
   Minimum Replicas:  1
   Ready Replicas:    2
   Replicas:          2
   Replicas Goal:     2
   Conditions:
   Last Transition Time:  2020-07-06T05:39:32.539Z
   Reason:                ServersReady
   Status:                True
   Type:                  Available
   Replicas:                2
   Servers:
   Desired State:  RUNNING
   Node Name:      aks-nodepool1-11471722-vmss000001
   Server Name:    admin-server
   State:          RUNNING
   Cluster Name:   cluster-1
   Desired State:  RUNNING
   Node Name:      aks-nodepool1-11471722-vmss000001
   Server Name:    managed-server1
   State:          RUNNING
   Cluster Name:   cluster-1
   Desired State:  RUNNING
   Node Name:      aks-nodepool1-11471722-vmss000001
   Server Name:    managed-server2
   State:          RUNNING
   Cluster Name:   cluster-1
   Desired State:  SHUTDOWN
   Server Name:    managed-server3
   Cluster Name:   cluster-1
   Desired State:  SHUTDOWN
   Server Name:    managed-server4
   Cluster Name:   cluster-1
   Desired State:  SHUTDOWN
   Server Name:    managed-server5
```
{{% /expand %}}

#### Domain debugging

Some suggestions for debugging problems with Model in Image after your Domain YAML file is deployed are found in the section on [debugging](/weblogic-kubernetes-operator/userguide/managing-domains/model-in-image/debugging/).

#### WSL2 bad timestamp

If you are running with WSL2, you may run into [bad timestamp issue](https://github.com/microsoft/WSL/issues/4245), which blocks Azure CLI. You may see the following error:

```shell
$ kubectl get pod
Unable to connect to the server: x509: certificate has expired or is not yet valid: current time 2020-11-25T15:58:10+08:00 is before 2020-11-27T04:25:04Z
```

You can run the following command to update WSL2 time system:

```
# Fix the outdated systime time
$ sudo hwclock -s

# Check systime time
$ data
Fri Nov 27 13:07:14 CST 2020
```

#### Timeout for the operator installation

You may run into timeout error while installing the operator and get the following error:

```
$ helm install weblogic-operator kubernetes/charts/weblogic-operator \
   --namespace sample-weblogic-operator-ns \
   --set serviceAccount=sample-weblogic-operator-sa \
   --set "enableClusterRoleBinding=true" \
   --set "domainNamespaceSelectionStrategy=LabelSelector" \
   --set "domainNamespaceLabelSelector=weblogic-operator\=enabled" \
--wait
Error: timed out waiting for the condition
```

Make sure your are working with branch master. Remove the operator and install again.

```bash
$ helm uninstall weblogic-operator -n sample-weblogic-operator-ns
release "weblogic-operator" uninstalled
```

Checkout master and install the operator.

```bash
$ git checkout master
$ cd weblogic-kubernetes-operator
$ helm install weblogic-operator kubernetes/charts/weblogic-operator \
   --namespace sample-weblogic-operator-ns \
   --set serviceAccount=sample-weblogic-operator-sa \
   --set "enableClusterRoleBinding=true" \
   --set "domainNamespaceSelectionStrategy=LabelSelector" \
   --set "domainNamespaceLabelSelector=weblogic-operator\=enabled" \
   --wait
```

#### WebLogic Image Tool failure

You will get an error running `./imagetool/bin/imagetool.sh` with docker buildkit enabled. 

Here shows the warning message:

```text
failed to solve with frontend dockerfile.v0: failed to create LLB definition: failed to parse stage name "WDT_BUILD": invalid reference format: repository name must be lowercase
```

To resolve the error, disable docker buildkit with the following commands and run the above `imagetool` command again. 

```bash
$ export DOCKER_BUILDKIT=0
$ export COMPOSE_DOCKER_CLI_BUILD=0
```

#### WebLogic Kubernetes Operator installation failure

Currently, we meet two cases that block the operator installation: 

* The system pods in the AKS cluster are pending. 
* The operator image is unavailable.

Follow the steps and dig into the error. 

##### The AKS cluster system pods are pending

If system pods in the AKS cluster are pending, it will block the operator installation.

This is an error example with warning message **no nodes available to schedule pods**.

```bash
$ kubectl get pod -A
NAMESPACE                     NAME                                        READY   STATUS    RESTARTS   AGE
default                       weblogic-operator-c5c78b8b5-ssvqk           0/1     Pending   0          13m
kube-system                   coredns-79766dfd68-wcmkd                    0/1     Pending   0          3h22m
kube-system                   coredns-autoscaler-66c578cddb-tc946         0/1     Pending   0          3h22m
kube-system                   dashboard-metrics-scraper-6f5fb5c4f-9f5mb   0/1     Pending   0          3h22m
kube-system                   kubernetes-dashboard-849d5c99ff-xzknj       0/1     Pending   0          3h22m
kube-system                   metrics-server-7f5b4f6d8c-bqzrn             0/1     Pending   0          3h22m
kube-system                   tunnelfront-765bf6df59-msj27                0/1     Pending   0          3h22m
sample-weblogic-operator-ns   weblogic-operator-f86b879fd-v2xrz           0/1     Pending   0          35m

$ kubectl describe pod weblogic-operator-f86b879fd-v2xrz -n sample-weblogic-operator-ns
...
Events:
  Type     Reason            Age                 From               Message
  ----     ------            ----                ----               -------
  Warning  FailedScheduling  71s (x25 over 36m)  default-scheduler  no nodes available to schedule pods
```

If you run into this error, remove the AKS cluster and create a new one. 

Run the `kubectl get pod -A` to make sure all the system pods are running.

```bash
$ kubectl get pod -A
NAMESPACE                     NAME                                        READY   STATUS    RESTARTS   AGE
kube-system                   coredns-79766dfd68-ch5b9                    1/1     Running   0          3h44m
kube-system                   coredns-79766dfd68-sxk4g                    1/1     Running   0          3h43m
kube-system                   coredns-autoscaler-66c578cddb-s5qm5         1/1     Running   0          3h44m
kube-system                   dashboard-metrics-scraper-6f5fb5c4f-wtckh   1/1     Running   0          3h44m
kube-system                   kube-proxy-fwll6                            1/1     Running   0          3h42m
kube-system                   kube-proxy-kq6wj                            1/1     Running   0          3h43m
kube-system                   kube-proxy-t2vbb                            1/1     Running   0          3h43m
kube-system                   kubernetes-dashboard-849d5c99ff-hrz2w       1/1     Running   0          3h44m
kube-system                   metrics-server-7f5b4f6d8c-snnbt             1/1     Running   0          3h44m
kube-system                   omsagent-8tf4j                              1/1     Running   0          3h43m
kube-system                   omsagent-n9b7k                              1/1     Running   0          3h42m
kube-system                   omsagent-rcmgr                              1/1     Running   0          3h43m
kube-system                   omsagent-rs-787ff54d9d-w7tp5                1/1     Running   0          3h44m
kube-system                   tunnelfront-794845c84b-v9f98                1/1     Running   0          3h44m
```