#!/usr/bin/env bash
# Copyright (c) 2018, 2021, Oracle and/or its affiliates.
# Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
#
# Description
#  This sample script creates a Azure Kubernetes Service cluster. 
#  It creates a new Azure resource group, with a new Azure Storage Account and Azure File Share to allow WebLogic 
#  to persist its configuration and data separately from the Kubernetes pods that run WebLogic workloads.
#
#  The Azure resource deployment is customized by editing
#  create-domain-on-aks-inputs.yaml. 
#
#  The following pre-requisites must be handled prior to running this script:
#    * Environment has set up, with git, azure cli, kubectl and helm installed.
#      See https://oracle.github.io/weblogic-kubernetes-operator/quickstart/get-images/
#    * The Azure Service Principal must have been created, with permission to
#      create AKS.

# Initialize
script="${BASH_SOURCE[0]}"
scriptDir="$( cd "$( dirname "${script}" )" && pwd )"

source ${scriptDir}/../common/utility.sh
source ${scriptDir}/../common/validate.sh

function usage {
  echo usage: ${script} -i file -o dir [-u uid] [-e] [-d] [-h]
  echo "  -i Parameter inputs file, must be specified."
  echo "  -o Output directory for the generated yaml files, must be specified."
  echo "  -u UID of resource, used to name file share, persistent valume, and persistent valume claim. "
  echo "  -e Also create the Azure Kubernetes Service cluster."
  echo "  -h Help"
  exit $1
}

#
# Parse the command line options
#
executeIt=false
while getopts "ehi:o:u:" opt; do
  case $opt in
    i) valuesInputFile="${OPTARG}"
    ;;
    o) outputDir="${OPTARG}"
    ;;
    u) azureResourceUID="${OPTARG}"
    ;;
    e) executeIt=true
    ;;
    h) usage 0
    ;;
    *) usage 1
    ;;
  esac
done

if [ -z ${valuesInputFile} ]; then
  echo "${script}: -i must be specified."
  missingRequiredOption="true"
fi

if [ -z ${outputDir} ]; then
  echo "${script}: -o must be specified."
  missingRequiredOption="true"
fi

if [ "${missingRequiredOption}" == "true" ]; then
  usage 1
fi

if [ -z "${azureResourceUID}" ];then 
  azureResourceUID=`date +%s`
fi

#
# Function to exit and print an error message
# $1 - text of message
function fail {
  echo [ERROR] $*
  exit 1
}

#
# Function to initialize and validate the output directory
# for the generated yaml files for this domain.
#
function initOutputDir {
  aksOutputDir="$outputDir/weblogic-on-aks"

  pvOutput="${aksOutputDir}/pv.yaml"
  pvcOutput="${aksOutputDir}/pvc.yaml"

  removeFileIfExists ${pvOutput}
  removeFileIfExists ${pvcOutput}
  removeFileIfExists ${aksOutputDir}/create-domain-on-aks-inputs.yaml
}

#
# Function to setup the environment to run the create Azure resource and domain job
#
function initialize {

  # Validate the required files exist
  validateErrors=false

  if [ -z "${valuesInputFile}" ]; then
    validationError "You must use the -i option to specify the name of the inputs parameter file (a modified copy of kubernetes/samples/scripts/create-weblogic-domain-on-aks/create-domain-on-aks-inputs.yaml)."
  else
    if [ ! -f ${valuesInputFile} ]; then
      validationError "Unable to locate the input parameters file ${valuesInputFile}"
    fi
  fi

  if [ -z "${outputDir}" ]; then
    validationError "You must use the -o option to specify the name of an existing directory to store the generated yaml files in."
  fi

  domainPVInput="${scriptDir}/azure-file-pv-template.yaml"
  if [ ! -f ${domainPVInput} ]; then
    validationError "The template file ${domainPVInput} for generating a persistent volume was not found"
  fi

  domainPVCInput="${scriptDir}/azure-file-pvc-template.yaml"
  if [ ! -f ${domainPVCInput} ]; then
    validationError "The template file ${domainPVCInput} for generating a persistent volume claim was not found"
  fi

  failIfValidationErrors

  # Parse the common inputs file
  parseCommonInputs
  initOutputDir
  failIfValidationErrors

  if [ ${#namePrefix} -gt 7 ]; then
    fail "namePrefix is allowed lowercase letters and numbers, between 1 and 7 characters."
  fi

  # Generate Azure resource name
  export azureResourceGroupName="${namePrefix}resourcegroup${azureResourceUID}"
  export aksClusterName="${namePrefix}akscluster${azureResourceUID}"
  export storageAccountName="${namePrefix}storage${azureResourceUID}"

  export azureFileShareSecretName="${namePrefix}${azureFileShareSecretNameSuffix}"
  export azureKubernetesNodepoolName="${azureKubernetesNodepoolNamePrefix}${namePrefix}"
  export azureStorageShareName="${namePrefix}-${azureStorageShareNameSuffix}-${azureResourceUID}"
  export persistentVolumeClaimName="${namePrefix}-${persistentVolumeClaimNameSuffix}"
}

#
# Function to generate the yaml files for creating Azure resources and WebLogic Server domain
#
function createYamlFiles {

  # Create a directory for this domain's output files
  mkdir -p ${aksOutputDir}

  # Make sure the output directory has a copy of the inputs file.
  # The user can either pre-create the output directory, put the inputs
  # file there, and create the domain from it, or the user can put the
  # inputs file some place else and let this script create the output directory
  # (if needed) and copy the inputs file there.
  copyInputsFileToOutputDirectory ${valuesInputFile} "${aksOutputDir}/create-domain-on-aks-inputs.yaml"

  echo Generating ${pvOutput}

  cp ${domainPVInput} ${pvOutput}
  sed -i -e "s:%PERSISTENT_VOLUME_NAME%:${persistentVolumeClaimName}:g" ${pvOutput}
  sed -i -e "s:%AZURE_FILE_SHARE_SECRET_NAME%:${azureFileShareSecretName}:g" ${pvOutput}
  sed -i -e "s:%AZURE_FILE_SHARE_NAME%:${azureStorageShareName}:g" ${pvOutput}
  sed -i -e "s:%STORAGE_CLASS_NAME%:${azureStorageClassName}:g" ${pvOutput}

  # Generate the yaml to create the persistent volume claim
  echo Generating ${pvcOutput}

  cp ${domainPVCInput} ${pvcOutput}
  sed -i -e "s:%PERSISTENT_VOLUME_CLAIM_NAME%:${persistentVolumeClaimName}:g" ${pvcOutput}
  sed -i -e "s:%STORAGE_CLASS_NAME%:${azureStorageClassName}:g" ${pvcOutput}

  # Remove any "...yaml-e" files left over from running sed
  rm -f ${aksOutputDir}/*.yaml-e
}

function loginAzure {
    # login with a service principal
    az login --service-principal --username $azureServicePrincipalAppId \
    --password $azureServicePrincipalClientSecret \
    --tenant $azureServicePrincipalTenantId
    echo Login Azure with Servie Principal successfully.

    if [ $? -ne 0 ]; then
      fail "Login to Azure failed!"
    fi
}

function createResourceGroup {
    # Create a resource group
    echo Check if ${azureResourceGroupName} exists
    ret=$(az group exists --name ${azureResourceGroupName})
    if [ $ret != false ];then 
      fail "${azureResourceGroupName} exists, please change value of namePrefix to generate a new resource group name."
    fi

    echo Creating Resource Group ${azureResourceGroupName}
    az group create --name $azureResourceGroupName --location $azureLocation
}

function createAndConnectToAKSCluster {
    # Create aks cluster
    echo Check if ${aksClusterName} exists
    ret=$(az aks list -g ${azureResourceGroupName} | grep "${aksClusterName}")
    if [ -n "$ret" ];then 
      fail "AKS instance with name ${aksClusterName} exists."
    fi

    echo Creating Azure Kubernetes Service ${aksClusterName}
    az aks create --resource-group $azureResourceGroupName \
    --name $aksClusterName \
    --vm-set-type VirtualMachineScaleSets \
    --node-count ${azureKubernetesNodeCount} \
    --generate-ssh-keys \
    --nodepool-name ${azureKubernetesNodepoolName} \
    --node-vm-size ${azureKubernetesNodeVMSize} \
    --location $azureLocation \
    --service-principal $azureServicePrincipalAppId \
    --client-secret $azureServicePrincipalClientSecret

    # Connect to AKS cluster
    echo Connencting to Azure Kubernetes Service.
    az aks get-credentials --resource-group $azureResourceGroupName --name $aksClusterName
}

function createFileShare {
    # Create a storage account
    echo Check if the storage account ${storageAccountName} exists.
    ret=$(az storage account check-name --name ${storageAccountName})
    nameAvailable=$(echo "$ret" | grep "nameAvailable" | grep "false")
    if [ -n "$nameAvailable" ];then 
      echo $ret
      fail "Storage account ${aksClusterName} is unavaliable."
    fi

    echo Creating Azure Storage Account ${storageAccountName}.
    az storage account create \
    -n $storageAccountName \
    -g $azureResourceGroupName \
    -l $azureLocation \
    --sku ${azureStorageAccountSku}

    # Export the connection string as an environment variable, this is used when creating the Azure file share
    export azureStorageConnectionString=$(az storage account show-connection-string \
    -n $storageAccountName -g $azureResourceGroupName -o tsv)

    # Create the file share
    echo Check if file share exists
    ret=$( az storage share exists --name ${azureStorageShareName} --account-name ${storageAccountName} --connection-string $azureStorageConnectionString | grep "exists" | grep false)
    if [[ $ret == true ]];then 
      fail "File share name  ${azureStorageShareName} is unavaliable."
    fi

    echo Creating Azure File Share ${azureStorageShareName}.
    az storage share create -n $azureStorageShareName \
    --connection-string $azureStorageConnectionString

    # Get storage account key
    azureStorageKey=$(az storage account keys list --resource-group $azureResourceGroupName \
    --account-name $storageAccountName --query "[0].value" -o tsv)

    # Echo storage account name and key
    echo Storage account name: $storageAccountName

    # Create a Kubernetes secret
    echo Creating kubectl secret for Azure File Share ${azureFileShareSecretName}.
    bash $dirKuberetesSecrets/create-azure-storage-credentials-secret.sh \
      -s ${azureFileShareSecretName} \
      -a $storageAccountName \
      -k $azureStorageKey

    # Mount the file share as a volume
    echo Mounting file share as a volume.
    kubectl apply -f ${pvOutput}
    kubectl get pv ${persistentVolumeClaimName} -o yaml
    kubectl apply -f ${pvcOutput}
    kubectl get pvc ${persistentVolumeClaimName} -o yaml
}

function printSummary {
  if [ "${executeIt}" = true ]; then
    regionJsonExcerpt=`az group list --query "[?name=='${azureResourceGroupName}']" | grep location`
    tokens=($(IFS='"'; for word in $regionJsonExcerpt; do echo "$word"; done))
    region=${tokens[2]}
    echo ""
    echo ""
    echo "The following Azure Resouces have been created: "
    echo "  Resource groups: ${azureResourceGroupName}, MC_${azureResourceGroupName}_${aksClusterName}_${region}"
    echo "  Kubernetes service cluster name: ${aksClusterName}"
    echo "  Storage account: ${storageAccountName}"
    echo "  Persistent Volumn name: ${persistentVolumeClaimName}"
    echo "  Persistent Volumn Clain name: ${persistentVolumeClaimName}"
    echo ""
    echo "Connect your kubectl to this cluster with this command:"
    echo "  az aks get-credentials --resource-group ${azureResourceGroupName} --name ${aksClusterName}"
    echo ""
  fi

  echo ""
  echo "The following files were generated:"
  echo "  ${pvOutput}"
  echo "  ${pvcOutput}"
  echo ""
  
  echo "Completed"
}

cd ${scriptDir}

cd ..
export dirSampleScripts=`pwd`
export dirKuberetesSecrets="${dirSampleScripts}/create-kuberetes-secrets"

cd ${scriptDir}

#
# Do these steps to create Azure resources and a WebLogic Server domain.
#

# Setup the environment for running this script and perform initial validation checks
initialize

# Generate the yaml files for creating the domain
createYamlFiles

# All done if the execute option is true
if [ "${executeIt}" = true ]; then

  # Login Azure with service pricipal
  loginAzure

  # Create resource group
  createResourceGroup

  # Create Azure Kubernetes Service and connect to AKS cluster
  createAndConnectToAKSCluster

  # Create File Share
  createFileShare
fi

# Print summary
printSummary
