#!/bin/bash
# Copyright 2019, Oracle Corporation and/or its affiliates. All rights reserved.
# Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

#
# This is an example of how to setup the WebLogic Kubernetes Cluster
#
# Expects the following env vars to already be set:
#
#   WORKDIR - working directory for the sample with at least 10g of space
#   WDT_DOMAIN_TYPE - WLS, RestrictedJRF, or JRF
#

set -eu

cd ${WORKDIR}

echo "@@ Info: Creating weblogic domain secret"

kubectl -n sample-domain1-ns \
  create secret generic sample-domain1-weblogic-credentials \
  --from-literal=username=weblogic \
  --from-literal=password=welcome1

kubectl -n sample-domain1-ns \
  label secret sample-domain1-weblogic-credentials \
  weblogic.domainUID=sample-domain1 \
  weblogic.domainName=sample-domain1

echo "@@ Info: Creating rcu access secret (ignored unless domain type is JRF or RestrictedJRF)"

kubectl -n sample-domain1-ns \
  create secret generic sample-domain1-rcu-access \
  --from-literal=rcu_prefix=FMW1 \
  --from-literal=rcu_schema_password=Oradoc_db1 \
  --from-literal=rcu_admin_password=Oradoc_db1 \
  --from-literal=rcu_db_conn_string=oracle-db.default.svc.cluster.local:1521/devpdb.k8s

kubectl -n sample-domain1-ns \
  label secret sample-domain1-rcu-access \
  weblogic.domainUID=sample-domain1 \
  weblogic.domainName=sample-domain1

echo "@@ Info: Creating sample wdt configmap (optional)"

kubectl -n sample-domain1-ns \
  create configmap wdt-config-map \
  --from-file=model1.20.properties

kubectl -n sample-domain1-ns \
  label  configmap wdt-config-map \
  weblogic.domainUID=sample-domain1 \
  weblogic.domainName=sample-domain1

kubectl -n sample-domain1-ns \
  create secret generic sample-domain1-opss-secrets \
  --from-literal=passphrase=welcome1 

kubectl -n sample-domain1-ns \
  label secret sample-domain1-opss-secrets \
  weblogic.domainUID=sample-domain1 \
  weblogic.domainName=sample-domain1

echo "@@ Info: Creating 'k8s-domain.yaml' from 'k8s-domain.yaml.template' and setting its Domain Type"

if [ ! "${WDT_DOMAIN_TYPE}" == "WLS" ] \
   && [ ! "${WDT_DOMAIN_TYPE}" == "RestrictedJRF" ] \
   && [ ! "${WDT_DOMAIN_TYPE}" == "JRF"]; then
  echo "Invalid domain type WDT_DOMAIN_TYPE '$WDT_DOMAIN_TYPE': expected 'WLS', 'JRF', or 'RestrictedJRF'." && exit 1
fi

cp k8s-domain.yaml.template k8s-domain.yaml
sed -i s/@@DOMTYPE@@/${WDT_DOMAIN_TYPE}/ k8s-domain.yaml

if [ "${WDT_DOMAIN_TYPE}" == "JRF" ] ; then
  sed -i 's/\#opssKeyPassPhrase:/opssKeyPassPhrase:/' k8s-domain.yaml
  sed -i 's/\#  name: sample-domain1-opss-secrets/  name: sample-domain1-opss-secrets/' k8s-domain.yaml
fi

echo "@@ Info: Applying domain resource yaml 'k8s-domain.yaml'"

kubectl apply -f k8s-domain.yaml

echo "@@ Info: Getting pod status - ctrl-c when all is running and ready to exit"

kubectl get pods -n sample-domain1-ns --watch
