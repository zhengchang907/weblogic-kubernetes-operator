#!/bin/bash
cd /operator
echo 'Wait until the operator pod creates certificates in the config map...'
while [ ! -f /operator/config/internalOperatorCert ] ;
do
      sleep 2
done
echo 'Update out trusted certs...'
cp /operator/config/internalOperatorCert /usr/local/share/ca-certificates/internalOperatorCert.crt
update-ca-certificates
echo 'Start the web app...'
export K8S_CACERT='/var/run/secrets/kubernetes.io/serviceaccount/ca.crt'
export K8S_TOKEN=`cat /var/run/secrets/kubernetes.io/serviceaccount/token`
export K8S_MASTER="https://kubernetes.default.svc"
npm start