---
title: "Domain home on a PV"
date: 2020-07-12T18:22:31-05:00
weight: 2
description: "Sample for creating a WebLogic domain home on an existing PV or PVC on the Azure Kubernetes Service."
---

This sample demonstrates how to use the [Oracle WebLogic Server Kubernetes Operator](/weblogic-kubernetes-operator/) (hereafter "the operator") to set up a WebLogic Server (WLS) cluster on the Azure Kubernetes Service (AKS). After going through the steps, your WLS domain runs on an AKS cluster instance and you can manage your WLS domain by accessing the WebLogic Server Administration Console.

#### Contents

 - [Prerequisites](#prerequisites)
    - [Create an AKS cluster](#create-an-azure-kubernetes-service-cluster)
    - [Oracle Container Registry](#oracle-container-registry)
 - [Install WebLogic Server Kubernetes Operator](#install-weblogic-server-kubernetes-operator)
 - [Create WebLogic domain](#create-weblogic-domain)
 - [Automation](#automation)
 - [Deploy sample application](#deploy-sample-application)
 - [Access WebLogic Server logs](#access-weblogic-server-logs)
 - [Clean up resources](#clean-up-resources)
 - [Troubleshooting](#troubleshooting) 
 - [Useful links](#useful-links)

#### Prerequisites

This sample assumes the following prerequisite environment setup.

* Operating System: GNU/Linux, macOS or [WSL for Windows 10](https://docs.microsoft.com/windows/wsl/install-win10).
* [Git](https://git-scm.com/downloads), use `git --version` to test if `git` works.  This document was tested with version 2.17.1.
* [Azure CLI](https://docs.microsoft.com/cli/azure), use `az --version` to test if `az` works.  This document was tested with version 2.9.1.
* [kubectl](https://kubernetes-io-vnext-staging.netlify.com/docs/tasks/tools/install-kubectl/), use `kubectl version` to test if `kubectl` works.  This document was tested with version v1.16.3.
* [helm](https://helm.sh/docs/intro/install/), version 3.1 and later, use `helm version` to check the `helm` version.  This document was tested with version v3.2.4.

##### Create an Azure Kubernetes Service cluster 

Create the Azure Kubernetes Service cluster using the Azure CLI. Follow [Create an AKS cluster]({{< relref "/samples/simple/azure-kubernetes-service/create-aks-cluster.md" >}}) to set up your Kubernetes cluster. After your Kubernetes cluster is up and running, run the following commands to make sure kubectl can access the Kubernetes cluster:

```shell
$ kubectl get nodes -o wide
NAME                                  STATUS   ROLES   AGE     VERSION    INTERNAL-IP   EXTERNAL-IP   OS-IMAGE             KERNEL-VERSION      CONTAINER-RUNTIME
aks-pool1haiche-33688868-vmss000000   Ready    agent   4m25s   v1.17.13   10.240.0.4    <none>        Ubuntu 16.04.7 LTS   4.15.0-1098-azure   docker://19.3.12
aks-pool1haiche-33688868-vmss000001   Ready    agent   4m12s   v1.17.13   10.240.0.5    <none>        Ubuntu 16.04.7 LTS   4.15.0-1098-azure   docker://19.3.12
```

##### Oracle Container Registry

You will need an Oracle account. The following steps will direct you to accept the license agreement for WebLogic Server.  Make note of your Oracle Account password and email.  This sample pertains to 12.2.1.4, but other versions may work as well.

  - In a web browser, navigate to https://container-registry.oracle.com and log in using the Oracle Single Sign-On authentication service. If you do not already have SSO credentials, at the top of the page, click the Sign In link to create them.
  - The Oracle Container Registry provides a WebLogic Server 12.2.1.4.0 Docker image, which already has the necessary patches applied, and the Oracle WebLogic Server 12.2.1.4.0 and 14.1.1.0.0 images, which do not require any patches.

{{% notice info %}} The following sections of the sample instructions will guide you, step-by-step, through the process of setting up a WebLogic cluster on AKS - remaining as close as possible to a native Kubernetes experience. This lets you understand and customize each step. If you wish to have a more automated experience that abstracts some lower level details, you can skip to the [Automation](#automation) section.
{{% /notice %}}


#### Install WebLogic Server Kubernetes Operator

The Oracle WebLogic Server Kubernetes Operator is an adapter to integrate WebLogic Server and Kubernetes, allowing Kubernetes to serve as a container infrastructure hosting WLS instances.  The operator runs as a Kubernetes Pod and stands ready to perform actions related to running WLS on Kubernetes.

Clone the [Oracle WebLogic Server Kubernetes Operator repository](https://github.com/oracle/weblogic-kubernetes-operator) to your machine. We will use several scripts in this repository to create a WebLogic domain. This sample was tested with v3.0.3.

```bash
$ git clone https://github.com/oracle/weblogic-kubernetes-operator.git
#cd weblogic-kubernetes-operator
#TODO: we have to fix the branch after the source code are merged
#git checkout v3.1.1
```

Kubernetes Operators use [Helm](https://helm.sh/) to manage Kubernetes applications. The operator’s Helm chart is located in the `kubernetes/charts/weblogic-operator` directory. Please install the operator by running the corresponding command.

```bash
$ helm repo add weblogic-operator https://oracle.github.io/weblogic-kubernetes-operator/charts
$ helm repo update
$ helm install weblogic-operator weblogic-operator/weblogic-operator --version "3.0.3"
```

The output will show something similar to the following:

```bash
$ helm install weblogic-operator weblogic-operator/weblogic-operator --version "3.0.3"
NAME: weblogic-operator
LAST DEPLOYED: Wed Jul  1 23:47:44 2020
NAMESPACE: default
STATUS: deployed
REVISION: 1
TEST SUITE: None
```

Verify the operator with the following command; the status will be running.

```bash
$ kubectl get pods -w
NAME                                              READY   STATUS      RESTARTS   AGE
weblogic-operator-56654bcdb7-qww7f                1/1     Running     0          25m
```

{{% notice tip %}} You will have to press Ctrl-C to exit this command due to the `-w` flag.
{{% /notice %}}

#### Create WebLogic domain

  - [Create secrets](#create-secrets)
  - [Create WebLogic Domain](#create-weblogic-domain-1)
  - [Understanding your first archive](#understanding-your-first-archive)
  - [Staging a ZIP file of the archive](#staging-a-zip-file-of-the-archive)
  - [Staging model files](#staging-model-files)
  - [Creating the image with WIT](#creating-the-image-with-wit)

Now that we have created the AKS cluster, installed the operator, and verified that the operator is ready to go, we can have the operator create a WLS domain.

##### Create secrets

We will use the `kubernetes/samples/scripts/create-weblogic-domain-credentials/create-weblogic-credentials.sh` script to create the domain credentials as a Kubernetes secret.

```bash
#cd kubernetes/samples/scripts/create-weblogic-domain-credentials
$ ./create-weblogic-credentials.sh -u weblogic -p welcome1 -d domain1
secret/domain1-weblogic-credentials created
secret/domain1-weblogic-credentials labeled
The secret domain1-weblogic-credentials has been successfully created in the default namespace.
```

We will use the `kubernetes/samples/scripts/create-kuberetes-secrets/create-docker-credentials-secret.sh` script to create the Docker credentials as a Kubernetes secret. Please run:

```bash
# Please change imagePullSecretNameSuffix if you change pre-defined value "regcred" before generating the configuration files.
$ export SECRET_NAME_DOCKER="regcred"

#cd kubernetes/samples/scripts/create-kuberetes-secrets
$ ./create-docker-credentials-secret.sh -s ${SECRET_NAME_DOCKER} -e oracleSsoEmail@bar.com -p oracleSsoPassword -u oracleSsoEmail@bar.com
secret/regcred created
The secret regcred has been successfully created in the default namespace.
```

Verify secrets with the following command:

```bash
$ kubectl get secret
NAME                                      TYPE                                  DATA   AGE
wlsazure-secret                           Opaque                                2      17m
regcred                                   kubernetes.io/dockerconfigjson        1      2m25s
default-token-csdvd                       kubernetes.io/service-account-token   3      25m
domain1-weblogic-credentials              Opaque                                2      3m42s
sh.helm.release.v1.weblogic-operator.v1   helm.sh/release.v1                    1      5m41s
weblogic-operator-secrets                 Opaque                                1      5m41s
```

> **Note**: If the `NAME` column in your output is missing any of the values shown above, please reexamine your execution of the preceding steps in this sample to ensure that you correctly followed all of them.  The `default-token-mwdj8` shown above will have a different ending in your output.

##### Create WebLogic Domain
We will use the `kubernetes/samples/scripts/create-weblogic-domain/domain-home-on-pv/create-domain.sh` script to create the WLS domain in the persistent volume we created previously.

We need to set up the domain configuration for the WebLogic domain.

1. Check if resources are ready

   If you used the automation script to create the AKS cluster, skip this step and go to step 2.

   If you create Azure resources step by step according to [Create an AKS cluster]({{< relref "/samples/simple/azure-kubernetes-service/create-aks-cluster.md" >}}), validate all the resources created above using the script `kubernetes/samples/scripts/create-weblogic-domain-on-azure-kubernetes-service/validate.sh`.

   Use the following commands to check if the resources are ready:

   ```bash
   #cd kubernetes/samples/scripts/create-weblogic-domain-on-azure-kubernetes-service
   $ ./validate.sh -g ${AKS_PERS_RESOURCE_GROUP} \
      --aks-name ${AKS_CLUSTER_NAME} \
      --file-share ${AKS_PERS_SHARE_NAME} \
      --storage-account ${AKS_PERS_STORAGE_ACCOUNT_NAME} \
      --domain-uid domain1 \
      --pv-name ${NAME_PREFIX}-azurefile \
      --pvc-name ${NAME_PREFIX}-azurefile \
      --secret-docker ${SECRET_NAME_DOCKER} \
      --secret-storage ${SECRET_NAME_AZURE_FILE}
   ```

   You will see output with `PASS` if all the resources are ready. The following is an example of output:

   ```text
   PASS
   You can create your domain with the following resources ready:
     Azure resource group: wlsresourcegroup1597391432
     Azure Kubenetes Service instacne: wlsaks1597391432
     Azure storage account: wlsstorage1597391432
     Azure file share: wls-weblogic-1597391432
     Kubenetes secret for Azure storage: wlsazure-secret
     Kubenetes secret for Docker Account: regcred
     Kubenetes secret for Weblogic domain: domain1-weblogic-credentials
     Persistent Volume: wls-azurefile
     Persistent Volume Claim: wls-azurefile
   ```

2. Follow [Domain home on a PV - Use the script to create a domain]({{< relref "/samples/simple/domains/domain-home-on-pv#use-the-script-to-create-a-domain" >}}) to create the WebLogic domain home within the AKS cluster.

   The domain creation inputs can be customized by editing input file. We provided sample input values in `kubernetes/samples/scripts/create-weblogic-domain-on-azure-kubernetes-service/domain-on-pv/create-domain-inputs.yaml`.

   Copy the sample input file to `~/azure/weblogic-on-aks`. If you want to edit your copy, please make sure the following values must be specified:

   | Name in YAML file | Example value | Notes |
   |-------------------|---------------|-------|
   |`imagePullSecretName`|`regcred`|Remove `#` at the beginning of `imagePullSecretName`, and make sure its value is the same value with `${SECRET_NAME_DOCKER}`|
   |`persistentVolumeClaimName`|`wls-azurefile`|Make sure it must be the same value with `${NAME_PREFIX}-azurefile` in [Create an AKS cluster]({{< relref "/samples/simple/azure-kubernetes-service/create-aks-cluster.md" >}}). If you created the AKS cluster with automation script, you can copy the PVC name from output of "Persistent Volumn Clain name".|
   |`exposeAdminNodePort`|`true`|Expose the admin port, we will use Administration Console to deploy a sample application. |


   ```bash
   #cd kubernetes/samples/scripts/create-weblogic-domain-on-azure-kubernetes-service/domain-on-pv
   $ if [ ! -d ~/azure/weblogic-on-aks ]; then mkdir ~/azure/weblogic-on-aks; fi
   $ cp create-domain-inputs.yaml ~/azure/weblogic-on-aks/my-create-domain-inputs.yaml

   #cd kubernetes/samples/scripts/create-weblogic-domain/domain-home-on-pv
   $ ./create-domain.sh -i ~/azure/weblogic-on-aks/my-create-domain-inputs.yaml -o ~/azure -e -v
   ```

   You may observe error-related output during the creation of the domain.  This is due to timing issues during domain creation.  The script accounts for this with a series of retries.  The error output looks similar to the following:

   ```text
   Waiting for the job to complete...
   Error from server (BadRequest): container "create-weblogic-sample-domain-job" in pod "domain1-create-weblogic-sample-domain-job-4l767" is waiting to start: PodInitializing
   status on iteration 1 of 20
   pod domain1-create-weblogic-sample-domain-job-4l767 status is Init:0/1
   status on iteration 2 of 20
   pod domain1-create-weblogic-sample-domain-job-4l767 status is Running
   ```

   If you see error messages that include the status `ImagePullBackOff` along with output similar to the following, it is likely your credentials for the Oracle Container Registry have not been successfully conveyed to the AKS cluster.

   ```bash
   Failed to pull image "container-registry.oracle.com/middleware/weblogic:12.2.1.4": rpc error: code = Unknown desc = Error response from daemon: Get https://container-registry-phx.oracle.com/v2/middleware/weblogic/manifests/12.2.1.4: unauthorized: authentication required
   ```

   Ensure the arguments you passed to the script `create-docker-credentials-secret.sh` are correct with respect to your Oracle SSO credentials.

   The following example output shows the WebLogic domain was created successfully.

   {{%expand "Click here to view the example output." %}} 
   ```bash
   $ ./create-domain.sh -i ~/azure/weblogic-on-aks/my-create-domain-inputs.yaml -o ~/azure -e -v
   Input parameters being used
   export version="create-weblogic-sample-domain-inputs-v1"
   export adminPort="7001"
   export adminServerName="admin-server"
   export domainUID="domain1"
   export domainHome="/shared/domains/domain1"
   export serverStartPolicy="IF_NEEDED"
   export clusterName="cluster-1"
   export configuredManagedServerCount="5"
   export initialManagedServerReplicas="2"
   export managedServerNameBase="managed-server"
   export managedServerPort="8001"
   export image="store/oracle/weblogic:12.2.1.4"
   export imagePullPolicy="IfNotPresent"
   export imagePullSecretName="regcred"
   export productionModeEnabled="true"
   export weblogicCredentialsSecretName="domain1-weblogic-credentials"
   export includeServerOutInPodLog="true"
   export logHome="/shared/logs/domain1"
   export httpAccessLogInLogHome="true"
   export t3ChannelPort="30012"
   export exposeAdminT3Channel="false"
   export adminNodePort="30701"
   export exposeAdminNodePort="true"
   export namespace="default"
   javaOptions=-Dweblogic.StdoutDebugEnabled=false
   export persistentVolumeClaimName="wls-azurefile"
   export domainPVMountPath="/shared"
   export createDomainScriptsMountPath="/u01/weblogic"
   export createDomainScriptName="create-domain-job.sh"
   export createDomainFilesDir="wlst"
   export serverPodMemoryRequest="768Mi"
   export serverPodCpuRequest="250m"
   export istioEnabled="false"
   export istioReadinessPort="8888"

   Generating /home/username/azure/weblogic-domains/domain1/create-domain-job.yaml
   Generating /home/username/azure/weblogic-domains/domain1/delete-domain-job.yaml
   Generating /home/username/azure/weblogic-domains/domain1/domain.yaml
   Checking to see if the secret domain1-weblogic-credentials exists in namespace default
   Checking if the persistent volume claim wls-azurefile in NameSpace default exists
   The persistent volume claim wls-azurefile already exists in NameSpace default
   Wwls 07:15:52.866794   53745 helpers.go:535] --dry-run is deprecated and can be replaced with --dry-run=client.
   configmap/domain1-create-weblogic-sample-domain-job-cm created
   Checking the configmap domain1-create-weblogic-sample-domain-job-cm was created
   configmap/domain1-create-weblogic-sample-domain-job-cm labeled
   Checking if object type job with name domain1-create-weblogic-sample-domain-job exists
   No resources found in default namespace.
   Creating the domain by creating the job /home/weblogic/azure/weblogic-domains/domain1/create-domain-job.yaml
   job.batch/domain1-create-weblogic-sample-domain-job created
   Waiting for the job to complete...
   Error from server (BadRequest): container "create-weblogic-sample-domain-job" in pod "domain1-create-weblogic-sample-domain-job-4l767" is waiting to start: PodInitializing
   status on iteration 1 of 20
   pod domain1-create-weblogic-sample-domain-job-4l767 status is Init:0/1
   status on iteration 2 of 20
   pod domain1-create-weblogic-sample-domain-job-4l767 status is Running
   status on iteration 3 of 20
   pod domain1-create-weblogic-sample-domain-job-4l767 status is Completed
   domain.weblogic.oracle/domain1 created

   Domain domain1 was created and will be started by the WebLogic Kubernetes Operator

   Administration console access is available at http://wlswls1596-wlsresourcegrou-685ba0-7434b4f5.hcp.eastus.azmk8s.io:30701/console
   The following files were generated:
     /home/username/azure/weblogic-domains/domain1/create-domain-inputs.yaml
     /home/username/azure/weblogic-domains/domain1/create-domain-job.yaml
     /home/username/azure/weblogic-domains/domain1/domain.yaml

   Completed
   ```
   {{% /expand %}}

   > **Note**: If your output does not show a successful completion, you must
   troubleshoot the reason and resolve it before proceeding to the next
   step.

4. You must create `LoadBalancer` services for the Administration Server and the WLS cluster.  This enables WLS to service requests from outside the AKS cluster.

   Use the sample configuration file `kubernetes/samples/scripts/create-weblogic-domain-on-azure-kubernetes-service/domain-on-pv/admin-lb.yaml` to create a load balancer service for the Administration Server. If you are choosing not to use the predefined YAML file and instead created new one with customized values, then substitute the following content with you domain values.

   ```yaml
   apiVersion: v1
   kind: Service
   metadata:
     name: domain1-admin-server-external-lb
     namespace: default
   spec:
     ports:
     - name: default
       port: 7001
       protocol: TCP
       targetPort: 7001
     selector:
       weblogic.domainUID: domain1
       weblogic.serverName: admin-server
     sessionAffinity: None
     type: LoadBalancer
   ```

   Use the sample configuration file `kubernetes/samples/scripts/create-weblogic-domain-on-azure-kubernetes-service/domain-on-pv/cluster-lb.yaml` to create a load balancer service for the managed servers. If you are choosing not to use the predefined YAML file and instead created new one with customized values, then substitute the following content with you domain values.

   ```yaml
   apiVersion: v1
   kind: Service
   metadata:
     name: domain1-cluster-1-lb
     namespace: default
   spec:
     ports:
     - name: default
       port: 8001
       protocol: TCP
       targetPort: 8001
     selector:
       weblogic.domainUID: domain1
       weblogic.clusterName: cluster-1
     sessionAffinity: None
     type: LoadBalancer
   ```

   Create the load balancer service using the following command:

   ```bash
   $ cd kubernetes/samples/scripts/create-weblogic-domain-on-azure-kubernetes-service/domain-on-pv
   $ kubectl apply -f admin-lb.yaml
   service/domain1-admin-server-external-lb created

   $ kubectl  apply -f cluster-lb.yaml
   service/domain1-cluster-1-external-lb created
   ```

   After a short time, you will see the Administration Server and Managed Servers running.

   Use the following command to check server pod status:

   ```bash
   $ kubectl get pods --watch
   ```

   It may take you up to 20 minutes to deploy all pods, please wait and make sure everything is ready.

   You can tail the logs of the Administration Server with this command:

   ```bash
   kubectl logs -f domain1-admin-server
   ```

   The final example of pod output is as following:

   ```bash
   $ kubectl get pods --watch
   NAME                                              READY   STATUS      RESTARTS   AGE
   domain1-admin-server                              1/1     Running     0          11m
   domain1-create-weblogic-sample-domain-job-4l767   0/1     Completed   0          13m
   domain1-managed-server1                           1/1     Running     0          3m56s
   domain1-managed-server2                           1/1     Running     0          3m56s
   weblogic-operator-56654bcdb7-qww7f                1/1     Running     0          25m
   ```

   {{% notice tip %}} If Kubernetes advertises the WebLogic pod as `Running` you can be assured the WebLogic Server actually is running because the operator ensures the Kubernetes health checks are actually polling the WebLogic health check mechanism.
   {{% /notice %}}

   Get the addresses of the Administration Server and Managed Servers (please wait for the external IP addresses to be assigned):

   ```bash
   $ kubectl get svc --watch
   ```

   The final example of service output is as following:

   ```bash
   $ kubectl get svc --watch
   NAME                               TYPE           CLUSTER-IP    EXTERNAL-IP      PORT(S)              AGE
   domain1-admin-server               ClusterIP      None          <none>           30012/TCP,7001/TCP   2d20h
   domain1-admin-server-ext           NodePort       10.0.182.50   <none>           7001:30701/TCP       2d20h
   domain1-admin-server-external-lb   LoadBalancer   10.0.67.79    52.188.176.103   7001:32227/TCP       2d20h
   domain1-cluster-1-lb               LoadBalancer   10.0.112.43   104.45.176.215   8001:30874/TCP       2d17h
   domain1-cluster-cluster-1          ClusterIP      10.0.162.19   <none>           8001/TCP             2d20h
   domain1-managed-server1            ClusterIP      None          <none>           8001/TCP             2d20h
   domain1-managed-server2            ClusterIP      None          <none>           8001/TCP             2d20h
   internal-weblogic-operator-svc     ClusterIP      10.0.192.13   <none>           8082/TCP             2d22h
   kubernetes                         ClusterIP      10.0.0.1      <none>           443/TCP              2d22h
   ```

   In the example, the URL to access the Administration Server is: `http://52.188.176.103:7001/console`.  The default user name for the Administration Console is `weblogic` and the default password is `welcome1`.  Please change this for production deployments.

   If the WLS Administration Console is still not available, use `kubectl describe domain` to check domain status.

   ```bash
   $ kubectl describe domain domain1
   ```

   Make sure the status of cluster-1 is `ServersReady` and `Available`.
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

#### Automation

If you want to automate the above steps of creating AKS cluster and WLS domain, you can use the script `kubernetes/samples/scripts/create-weblogic-domain-on-azure-kubernetes-service/create-domain-on-aks.sh` in `v3.0.3`.

The sample script will create a WLS domain home on the AKS cluster, including
  - creating a new Azure resource group, with a new Azure Storage Account and Azure File Share to allow WebLogic to persist its configuration and data separately from the Kubernetes pods that run WLS workloads.
  - creating WLS domain home.
  - generating the domain resource YAML files, which can be used to restart the Kubernetes artifacts of the corresponding domain.

Clone WebLogic Server Kubernetes Operator repository and checkout `v3.0.3`.

```bash
$ git clone https://github.com/oracle/weblogic-kubernetes-operator.git
$ cd weblogic-kubernetes-operator
$ git checkout v3.0.3
```

You must specify the input values, you can edit `kubernetes/samples/scripts/create-weblogic-domain-on-azure-kubernetes-service/create-domain-on-aks-inputs.yaml` directly, or copy the file and edit your copy. The following values must be specified:

| Name in YAML file | Example value | Notes |
|-------------------|---------------|-------|
| `azureServicePrincipalAppId` | `nr086o75-pn59-4782-no5n-nq2op0rsr1q6` | Application ID of your service principal, refer to the application ID in the [Create Service Principal]({{< relref "/samples/simple/azure-kubernetes-service/create-aks-cluster#create-service-principal-for-aks" >}}) section. |
| `azureServicePrincipalClientSecret` | `8693089o-q190-45ps-9319-or36252s3s90` | A client secret of your service principal, refer to the client secret in the [Create Service Principal]({{< relref "/samples/simple/azure-kubernetes-service/create-aks-cluster#create-service-principal-for-aks" >}}) section. |
| `azureServicePrincipalTenantId` | `72s988os-86s1-cafe-babe-2q7pq011qo47` | Tenant (Directory ) ID of your service principal, refer to the client secret in the [Create Service Principal]({{< relref "/samples/simple/azure-kubernetes-service/create-aks-cluster#create-service-principal-for-aks" >}}) section. |
| `dockerEmail` | `yourDockerEmail` | Oracle Single Sign-On (SSO) account email, used to pull the WebLogic Server Docker image. |
| `dockerPassword` | `yourDockerPassword`| Password for Oracle SSO account, used to pull the WebLogic Server Docker image.  In clear text. |
| `dockerUserName` | `yourDockerId` | The same value as `dockerEmail`.  |
| `namePrefix` | `0730` | Alphanumeric value used as a disambiguation prefix for several Kubernetes resources. |

If you don't want to change the other parameters, you can use the default values.  Please make sure no extra whitespaces are added!

```bash
# Use ~/azure as output directory, please change it according to your requirement.

# cd kubernetes/samples/scripts/create-weblogic-domain-on-azure-kubernetes-service
$ cp create-domain-on-aks-inputs.yaml my-create-domain-on-aks-inputs.yaml
$ ./create-domain-on-aks.sh -i my-create-domain-on-aks-inputs.yaml -o ~/azure -e
```

The script will print the Administration Server address after a successful deployment.  The default user name for the Administration Console is `weblogic` and the default password is `welcome1`.  Please change this for production deployments.  To interact with the cluster using `kubectl`, use `az aks get-credentials` as shown in the script output.

{{% notice info %}} You now have created an AKS cluster with `PersistentVolumeClaim` and `PersistentVolume` to contain the WLS domain configuration files.  Using those artifacts, you have used the operator to create a WLS domain.
{{% /notice %}}

#### Deploy sample application

Now that you have WLS running in AKS, you can test the cluster by deploying the simple sample application included in the repository:

1. Go to the WebLogic Server Administration Console, Select "Lock & Edit".
2. Select Deployments.
3. Select Install.
4. Select Upload your file(s).
5. For the Deployment Archive, Select "Choose File".
6. Select the file `kubernetes/samples/charts/application/testwebapp.war`.
7. Select Next. Choose 'Install this deployment as an application'.
8. Select Next. Select cluster-1 and All servers in the cluster.
9. Accept the defaults in the next screens and Select Finish.
10. Select Activate Changes.

{{%expand "Click here to view the application deployment screenshot." %}}
![Deploy Application](../screenshot-deploy-test-app.png)
{{% /expand %}}

Next you will need to start the application:

1. Go to Deployments
1. Select Control
1. Select testwebapp
1. Select Start
1. Select Servicing all requests
1. Select Yes.

After the successful deployment, go to the application through the domain1-cluster-1-lb external IP.

```bash
$ kubectl  get svc domain1-cluster-1-external-lb
NAME                            TYPE           CLUSTER-IP     EXTERNAL-IP     PORT(S)          AGE
domain1-cluster-1-external-lb   LoadBalancer   10.0.108.249   52.224.248.40   8001:32695/TCP   30m
```

In the example, the application address is: `http://52.224.248.40:8001/testwebapp`.

The test application will list the server host and server IP on the page.

#### Access WebLogic Server logs

The logs are stored in the Azure file share. Follow these steps to access the log:

1. Go to the [Azure Portal](https://ms.portal.azure.com).
2. Go to your resource group.
3. Open the storage account.
4. In the "File service" section of the left panel, select File shares.
5. Select the file share name (e.g. weblogic in this example).
6. Select logs.
7. Select domain1.
8. WebLogic Server logs are listed in the folder.

{{%expand "Click here to view the WebLogic Server logs screenshot." %}}
![WebLogic Server Logs](../screenshot-logs.png)
{{% /expand %}}

#### Clean up resources

[Clean up resource]({{< relref "/samples/simple/azure-kubernetes-service/create-aks-cluster#clean-up-resources" >}}): clean up Azure resources using Azure CLI

#### Troubleshooting

[Troubleshooting]({{< relref "/samples/simple/azure-kubernetes-service/troubleshooting.md" >}})

#### Useful links

- [Domain on a PV]({{< relref "/samples/simple/domains/domain-home-on-pv/_index.md" >}}) sample
