#!/bin/bash

echo 'Downloading the OCI Java SDK...'
wget -q https://github.com/oracle/oci-java-sdk/releases/download/v1.3.7/oci-java-sdk.zip

echo 'Extracting the library...'
jar xvf oci-java-sdk.zip lib/oci-java-sdk-full-1.3.7.jar

if [[ "$MAVEN" == "" ]]; then
  export MAVEN="mvn"
fi

echo 'Installing into local Maven repository...'
$MAVEN install:install-file \
       -Dfile=lib/oci-java-sdk-full-1.3.7.jar \
       -DgeneratePom=true \
       -DgroupId=com.oracle.oci \
       -DartifactId=java-sdk \
       -Dversion=1.3.7 \
       -Dpackaging=jar

rm lib/oci-java-sdk-full-1.3.7.jar oci-java-sdk.zip

echo 'Done'