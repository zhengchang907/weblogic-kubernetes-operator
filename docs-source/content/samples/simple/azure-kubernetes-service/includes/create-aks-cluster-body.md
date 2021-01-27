#### Create an Azure Kubernetes Service cluster

Follow the steps in this section to create an AKS cluster on which to run the WebLogic Kubernetes Operator and create WLS domains.

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

##### Oracle Container Registry

You will need an Oracle account. The following steps will direct you to accept the license agreement for WebLogic Server.  Make note of your Oracle Account password and email.  This sample pertains to 12.2.1.4, but other versions may work as well.

  - In a web browser, navigate to https://container-registry.oracle.com and log in using the Oracle Single Sign-On authentication service. If you do not already have SSO credentials, at the top of the page, click the **Sign In** link to create them.
  - The Oracle Container Registry provides a WebLogic Server 12.2.1.4.0 Docker image, which already has the necessary patches applied, and the Oracle WebLogic Server 12.2.1.4.0 and 14.1.1.0.0 images, which do not require any patches.
  - Find and then pull the WebLogic 12.2.1.4 install image:
     ```bash
     $ docker pull container-registry.oracle.com/middleware/weblogic:12.2.1.4
     ```
  
##### Create the AKS cluster

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

After your Kubernetes cluster is up and running, run the following commands to make sure kubectl can access the Kubernetes cluster:

```shell
$ kubectl get nodes -o wide
NAME                                  STATUS   ROLES   AGE     VERSION    INTERNAL-IP   EXTERNAL-IP   OS-IMAGE             KERNEL-VERSION      CONTAINER-RUNTIME
aks-pool1haiche-33688868-vmss000000   Ready    agent   4m25s   v1.17.13   10.240.0.4    <none>        Ubuntu 16.04.7 LTS   4.15.0-1098-azure   docker://19.3.12
aks-pool1haiche-33688868-vmss000001   Ready    agent   4m12s   v1.17.13   10.240.0.5    <none>        Ubuntu 16.04.7 LTS   4.15.0-1098-azure   docker://19.3.12
```

