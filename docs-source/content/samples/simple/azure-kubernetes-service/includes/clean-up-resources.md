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

