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
