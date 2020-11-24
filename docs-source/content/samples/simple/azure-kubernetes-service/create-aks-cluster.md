---
title: "Create an AKS cluster"
date: 2020-11-24T17:32:31-05:00
weight: 1
description: "Sample for creating an Azure Kubernetes Service cluster."
---

#### Contents

 - [Prerequisites](#prerequisites)
    - [Create a Service Principal](#create-service-principal-for-aks)
 - [Create an AKS cluster](#create-an-azure-kubernetes-service-cluster)
 - [Create storage](#create-storage)
    - [Create an Azure Storage account](#create-an-azure-storage-account)
    - [Create a PV and PVC](#create-storage)
 - [Automation](#automation)
 - [Clean up resources](#clean-up-resources)
 - [Useful links](#useful-links)

#### Prerequisites

* Operating System: GNU/Linux, macOS or [WSL for Windows 10](https://docs.microsoft.com/windows/wsl/install-win10).
* [Azure CLI](https://docs.microsoft.com/cli/azure), use `az --version` to test if `az` works.  This document was tested with version 2.9.1.
* [kubectl](https://kubernetes-io-vnext-staging.netlify.com/docs/tasks/tools/install-kubectl/), use `kubectl version` to test if `kubectl` works.  This document was tested with version v1.16.3.

##### Create a Service Principal for AKS

An AKS cluster requires either an [Azure Active Directory (AD) service principal](https://docs.microsoft.com/azure/active-directory/develop/app-objects-and-service-principals) or a [managed identity](https://docs.microsoft.com/azure/aks/use-managed-identity) to interact with Azure resources.

We will use a service principal to create an AKS cluster. Follow the commands below to create a new service principal.

Please run `az login` first. Do set the subscription you want to work with. You can get a list of your subscriptions by running `az account list`.

```bash
# Login
$ az login

# Set your working subscription
$ export SUBSCRIPTION_ID=<your-subscription-id>
$ az account set -s $SUBSCRIPTION_ID
```

Create the new service principal with the following commands:

```bash
# Create Service Principal
$ export SP_NAME=myAKSClusterServicePrincipal
$ az ad sp create-for-rbac --skip-assignment --name $SP_NAME

# Copy the output to a file, we will use it later.
```

If you see an error similar to the following:

```bash
Found an existing application instance of "5pn2s201-nq4q-43n1-z942-p9r9571qr3rp". We will patch it
Insufficient privileges to complete the operation.
```

The problem may be a pre-existing service principal with the same name.  Either delete the other Service Principal or pick a different name.

Successful output will look like the following:

```json
{
  "appId": "r3qnq743-61s9-4758-8163-4qpo87s72s54",
  "displayName": "myAKSClusterServicePrincipal",
  "name": "http://myAKSClusterServicePrincipal",
  "password": "TfhR~uOJ1C1ftD5NS_LzJJj6UOjS2OwXfz",
  "tenant": "82sr215n-0ns5-404e-9161-206r0oqyq999"
}
```

Grant your service principal with a contributor role to create AKS resources.

```bash
# Use the <appId> from the output of the last command
$ export SP_APP_ID=r3qnq743-61s9-4758-8163-4qpo87s72s54
$ az role assignment create --assignee $SP_APP_ID --role Contributor
```

Successful output will look like the following:

```json
{
  "canDelegate": null,
  "id": "/subscriptions/p7844r91-o11q-4n7s-np6s-996308sopqo9/providers/Microsoft.Authorization/roleAssignments/4oq396os-rs95-4n6s-n3qo-sqqpnpo91035",
  "name": "4oq396os-rs95-4n6s-n3qo-sqqpnpo91035",
  "principalId": "952551r8-n129-4on3-oqo9-231n0s6011n3",
  "principalType": "ServicePrincipal",
  "roleDefinitionId": "/subscriptions/p7844r91-o11q-4n7s-np6s-996308sopqo9/providers/Microsoft.Authorization/roleDefinitions/o24988np-6180-42n0-no88-20s7382qq24p",
  "scope": "/subscriptions/p7844r91-o11q-4n7s-np6s-996308sopqo9",
}
```

{{% notice info %}} The following sections will guide you, step-by-step, through the process of an AKS cluster - remaining as close as possible to a native Kubernetes experience. This lets you understand and customize each step. We highly recommend you a more automated experience that abstracts some lower level details, you can skip to the [Automation](#automation) section.
{{% /notice %}}

#### Create an Azure Kubernetes Service cluster

This sample requires that you disable the AKS addon `http_application_routing` by default.  If you want to enable  `http_application_routing`, please follow [HTTP application routing](https://docs.microsoft.com/azure/aks/http-application-routing).

Run the following commands to create the AKS cluster instance.

```bash
# Change these parameters as needed for your own environment
# Specify a prefix to name resources, only allow lowercase letters and numbers, between 1 and 7 characters
$ export NAME_PREFIX=wls
# Used to generate resource names.
$ export TIMESTAMP=`date +%s`
$ export AKS_CLUSTER_NAME="${NAME_PREFIX}aks${TIMESTAMP}"
$ export AKS_PERS_RESOURCE_GROUP="${NAME_PREFIX}resourcegroup${TIMESTAMP}"
$ export AKS_PERS_LOCATION=eastus
$ export SP_APP_ID=<appId from the az ad sp create-for-rbac command>
$ export SP_CLIENT_SECRET=<password from the az ad sp create-for-rbac command>

$ az group create --name $AKS_PERS_RESOURCE_GROUP --location $AKS_PERS_LOCATION
$ az aks create \
   --resource-group $AKS_PERS_RESOURCE_GROUP \
   --name $AKS_CLUSTER_NAME \
   --node-count 2 \
   --generate-ssh-keys \
   --nodepool-name nodepool1 \
   --node-vm-size Standard_DS2_v2 \
   --location $AKS_PERS_LOCATION \
   --service-principal $SP_APP_ID \
   --client-secret $SP_CLIENT_SECRET
```

Successful output will be a JSON object with the entry `"type": "Microsoft.ContainerService/ManagedClusters"`.

After the deployment finishes, run the following command to connect to the AKS cluster. This command updates your local `~/.kube/config` so that subsequent `kubectl` commands interact with the named AKS cluster.

```bash
$ az aks get-credentials --resource-group $AKS_PERS_RESOURCE_GROUP --name $AKS_CLUSTER_NAME
```

Successful output will look similar to:

```bash
Merged "wlsaks1596087429" as current context in /home/username/.kube/config
```

To verify the connection to your cluster, use the `kubectl get` command to return a list of the cluster nodes.

```bash
$ kubectl get nodes
```

Example output:

```bash
$ kubectl get nodes
NAME                                STATUS   ROLES   AGE     VERSION
aks-nodepool1-15992006-vmss000000   Ready    agent   7m49s   v1.15.11
aks-nodepool1-15992006-vmss000001   Ready    agent   7m32s   v1.15.11
aks-nodepool1-15992006-vmss000002   Ready    agent   7m52s   v1.15.11
```

#### Create storage

Our usage pattern for the operator involves creating Kubernetes "persistent volumes" to allow the WebLogic Server to persist its configuration and data separately from the Kubernetes Pods that run WebLogic Server workloads.

We will create an external data volume to access and persist data. There are several options for data sharing as described in [Storage options for applications in Azure Kubernetes Service (AKS)](https://docs.microsoft.com/azure/aks/concepts-storage).

We will use Azure Files as a Kubernetes volume. Consult the [Azure Files Documentation](https://docs.microsoft.com/azure/aks/azure-files-volume) for details about this full featured cloud storage solution.

##### Create an Azure Storage account

Create a storage account using Azure CLI. Note that the storage account name can contain only lowercase letters and numbers, and must be between 3 and 24 characters in length:

```bash
# Change the value as needed for your own environment
$ export AKS_PERS_STORAGE_ACCOUNT_NAME="${NAME_PREFIX}storage${TIMESTAMP}"

$ az storage account create \
   -n $AKS_PERS_STORAGE_ACCOUNT_NAME \
   -g $AKS_PERS_RESOURCE_GROUP \
   -l $AKS_PERS_LOCATION \
   --sku Standard_LRS
```

Successful output will be a JSON object with the entry `"type": "Microsoft.Storage/storageAccounts"`.

Now we need to create a file share. To create the file share, you need a storage connection string. Run the `show-connection-string` command to get connection string, then create the share with `az storage share create`, as shown here.

```bash
# Change value as needed for your own environment
$ export AKS_PERS_SHARE_NAME="${NAME_PREFIX}-weblogic-${TIMESTAMP}"
# Get connection string
$ export AZURE_STORAGE_CONNECTION_STRING=$(az storage account show-connection-string -n $AKS_PERS_STORAGE_ACCOUNT_NAME -g $AKS_PERS_RESOURCE_GROUP -o tsv)
# Create file share
$ az storage share create -n $AKS_PERS_SHARE_NAME --connection-string $AZURE_STORAGE_CONNECTION_STRING
```

Successful output will be exactly the following:

```bash
{
  "created": true
}
```

The operator uses Kubernetes Secrets.  We need a storage key for the secret. These commands query the storage account to obtain the key, and then stores the storage account key as a Kubernetes secret.

```bash
$ export STORAGE_KEY=$(az storage account keys list --resource-group $AKS_PERS_RESOURCE_GROUP --account-name $AKS_PERS_STORAGE_ACCOUNT_NAME --query "[0].value" -o tsv)
```

Verify the successful output by examining the `STORAGE_KEY` environment variable.  It must not be empty.  It must be a long ASCII string.

We will use the `kubernetes/samples/scripts/create-kuberetes-secrets/create-azure-storage-credentials-secret.sh` script to create the storage account key as a Kubernetes secret, naming the secret with value `${NAME_PREFIX}azure-secret`. Please run:

```bash
# Please change persistentVolumeClaimNameSuffix if you changed pre-defined value "regcred" before generating the configuration files.
$ export SECRET_NAME_AZURE_FILE="${NAME_PREFIX}azure-secret"

#cd kubernetes/samples/scripts/create-kuberetes-secrets
$ ./create-azure-storage-credentials-secret.sh -s $SECRET_NAME_AZURE_FILE -a $AKS_PERS_STORAGE_ACCOUNT_NAME -k $STORAGE_KEY
```

You will see the following output:

```text
secret/wlsazure-secret created
The secret wlsazure-secret has been successfully created in the default namespace.
```

##### Create PV and PVC

This sample uses Kubernetes Persistent Volume Claims (PVC) as storage resource.  These features are expressed to Kubernetes using YAML files.  The script `kubernetes/samples/scripts/create-weblogic-domain-on-azure-kubernetes-service/create-aks-cluster.sh` generates the required configuration files automatically, given an input file containing the parameters.  A parameters file is provided at `kubernetes/samples/scripts/create-weblogic-domain-on-azure-kubernetes-service/create-aks-cluster-inputs.yaml`.  Copy and customize this file for your needs.

To generate YAML files to create PV and PVC in the AKS cluster, the following values must be substituted in your copy of the input file.

| Name in YAML file | Example value | Notes |
|-------------------|---------------|-------|
| `namePrefix` | `wls` | Alphanumeric value used as a disambiguation prefix for several Kubernetes resources. Make sure the value matches the value of `${NAME_PREFIX}` to keep names in step-by-step commands the same with those in configuration files. |

Use the following command to generate configuration files, assuming the output directory is `~/azure`.  The script will overwrite any files generated by a previous invocation.

```bash
#cd kubernetes/samples/scripts/create-weblogic-domain-on-azure-kubernetes-service
$ cp create-aks-cluster-inputs.yaml my-create-aks-cluster-inputs.yaml
$ ./create-aks-cluster.sh -i my-create-aks-cluster-inputs.yaml -o ~/azure -u ${TIMESTAMP}
```

After running the command, all needed configuration files are generated and output to `~/azure/weblogic-on-aks`:

```bash
The following files were generated:
  /home/username/azure/weblogic-on-aks/pv.yaml
  /home/username/azure/weblogic-on-aks/pvc.yaml

Completed
```

{{% notice info %}} Beyond the required and default configurations generated by the command, you can modify the generated YAML files to further customize your deployment. Please consult the [operator documentation]({{< relref "/userguide/_index.md" >}}), [AKS documentation](https://docs.microsoft.com/en-us/azure/aks/) and Kubernetes references for further information about customizing your deployment.
{{% /notice %}}

In order to mount the file share as a persistent volume, we have provided a configuration file `pv.yaml`. You can find it in your output directory. The following content is an example that uses the value `wls-weblogic` as "shareName", `wlsazure-secret` as "secretName", and the persistent volume name is `wls-azurefile`.

We will use the storage class `azurefile`. If you want to create a new class, follow this document [Create a storage class](https://docs.microsoft.com/en-us/azure/aks/azure-files-dynamic-pv#create-a-storage-class). For more information, see the page [Storage options for applications in Azure Kubernetes Service (AKS)](https://docs.microsoft.com/en-us/azure/aks/concepts-storage#storage-classes).

```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: wls-azurefile
spec:
  capacity:
    storage: 5Gi
  accessModes:
    - ReadWriteMany
  storageClassName: azurefile
  azureFile:
    secretName: wlsazure-secret
    shareName: wls-weblogic-1597391432
    readOnly: false
  mountOptions:
  - dir_mode=0777
  - file_mode=0777
  - uid=1000
  - gid=1000
  - mfsymlinks
  - nobrl
```

We have provided another configuration file `pvc.yaml` for the PersistentVolumeClaim.  Both `pv.yaml` and `pvc.yaml` have exactly the same content for `storageClassName` attributes. This is required. We set the same value to the `metadata` property in both files. The following content is an example that uses the persistent volume claim name `wls-azurefile`.

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: wls-azurefile
spec:
  accessModes:
    - ReadWriteMany
  storageClassName: azurefile
  resources:
    requests:
      storage: 5Gi
```

Use the `kubectl` command to create the persistent volume and persistent volume claim to `default` namespace.

```bash
$ kubectl apply -f ~/azure/weblogic-on-aks/pv.yaml
persistentvolume/wls-azurefile created
$ kubectl apply -f ~/azure/weblogic-on-aks/pvc.yaml
persistentvolumeclaim/wls-azurefile created
```

Use the following command to verify:

```bash
$ kubectl get pv,pvc
```

Example output:

```bash
$ kubectl get pv,pvc
NAME                                          CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS   CLAIM                                STORAGECLASS   REASON   AGE
persistentvolume/wls-azurefile   5Gi        RWX            Retain           Bound    default/wls-azurefile   azurefile               16m

NAME                                               STATUS   VOLUME                       CAPACITY   ACCESS MODES   STORAGECLASS   AGE
persistentvolumeclaim/wls-azurefile   Bound    wls-azurefile   5Gi        RWX            azurefile      16m
```

> **Note**: Carefully inspect the output and verify it matches the above. `ACCESS MODES`, `CLAIM`, and `STORAGECLASS` are vital.

#### Automation

We provide automation script to create an AKS cluster, you can use the `kubernetes/samples/scripts/create-weblogic-domain-on-azure-kubernetes-service/create-aks-cluster.sh` script with the `-e` option. This option applies the YAML files generated by the script.

The script will:
  * create an Azure resource group for AKS and Azure Storage resources.
  * create an AKS cluster with properties that specified by your inputs.
  * create an Azure storage account and file share service.
  * create a PV and PVC using the Azure file share service. 

For input values, you can edit `kubernetes/samples/scripts/create-weblogic-domain-on-azure-kubernetes-service/create-aks-cluster-inputs.yaml` directly, or copy the file and edit your copy. The following values must be specified:

| Name in YAML file | Example value | Notes |
|-------------------|---------------|-------|
| `azureServicePrincipalAppId` | `nr086o75-pn59-4782-no5n-nq2op0rsr1q6` | Application ID of your service principal, refer to the application ID in the [Create Service Principal](#create-a-service-principal-for-aks) section. |
| `azureServicePrincipalClientSecret` | `8693089o-q190-45ps-9319-or36252s3s90` | A client secret of your service principal, refer to the client secret in the [Create Service Principal](#create-a-service-principal-for-aks) section. |
| `azureServicePrincipalTenantId` | `72s988os-86s1-cafe-babe-2q7pq011qo47` | Tenant (Directory ) ID of your service principal, refer to the client secret in the [Create Service Principal](#create-a-service-principal-for-aks) section. |
| `namePrefix` | `wls` | Alphanumeric value used as a disambiguation prefix for several Kubernetes resources. |

If you don't want to change the other parameters, you can use the default values.  Please make sure no extra whitespaces are added!

```bash
# Use ~/azure as output directory, please change it according to your requirement.

# cd kubernetes/samples/scripts/create-weblogic-domain-on-azure-kubernetes-service
$ cp create-aks-cluster-inputs.yaml my-create-aks-cluster-inputs.yaml
$ ./create-aks-cluster.sh -i my-create-aks-cluster-inputs.yaml -o ~/azure -e
```

The script will print the resource names after a successful deployment.  To interact with the cluster using `kubectl`, use `az aks get-credentials` as shown in the script output.

{{% notice info %}} You now have created an AKS cluster with `PersistentVolumeClaim` and `PersistentVolume` for storage in `default` namespace.  Using those artifacts, you can follow [Domain on a PV]({{< relref "/samples/simple/azure-kubernetes-service/domain-on-pv.md" >}}) or [Model in image]({{< relref "/samples/simple/azure-kubernetes-service/model-in-image.md" >}}) to configure the WebLogic Server domain for your scenario.
{{% /notice %}}

#### Clean up resources

The output from the `create-aks-cluster.sh` script includes a statement about the Azure resources created by the script.  To delete the cluster and free all related resources, simply delete the resource groups.  The output will list the resource groups, such as.

```bash
The following Azure resouces have been created:
  Resource groups: ejb8191resourcegroup1597641911, MC_ejb8191resourcegroup1597641911_ejb8191akscluster1597641911_eastus
```

Given the above output, the following Azure CLI commands will delete the resource groups.

```bash
az group delete --yes --no-wait --name ejb8191resourcegroup1597641911
az group delete --yes --no-wait --name MC_ejb8191resourcegroup1597641911_ejb8191akscluster1597641911_eastus
```

Alternatively, if you create the resource step by step, then run the following commands to clean up resources:

```bash
$ az group delete --yes --no-wait --name $AKS_PERS_RESOURCE_GROUP
$ az group delete --yes --no-wait --name "MC_$AKS_PERS_RESOURCE_GROUP_$AKS_CLUSTER_NAME_$AKS_PERS_LOCATION"
```


#### Useful links

- [Domain on a PV]({{< relref "/samples/simple/azure-kubernetes-service/domain-on-pv.md" >}}): running the WebLogic cluster on AKS with domain home on PV
- [Model in image]({{< relref "/samples/simple/azure-kubernetes-service/model-in-image.md" >}}): running the WebLogic cluster on AKS with domain model in image
- [Quickstart: Deploy an Azure Kubernetes Service cluster using the Azure CLI](https://docs.microsoft.com/azure/aks/kubernetes-walkthrough)
- [WebLogic Server Kubernetes Operator](https://oracle.github.io/weblogic-kubernetes-operator/userguide/introduction/introduction/)
- [Manually create and use a volume with Azure Files share in Azure Kubernetes Service (AKS)](https://docs.microsoft.com/azure/aks/azure-files-volume)
- [Create a Secret by providing credentials on the command line](https://kubernetes.io/docs/tasks/configure-pod-container/pull-image-private-registry/#create-a-secret-by-providing-credentials-on-the-command-line)