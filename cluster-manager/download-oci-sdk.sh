#!/bin/bash

echo 'Downloading the OCI Java SDK...'
wget -q https://github.com/oracle/oci-java-sdk/releases/download/v1.5.2/oci-java-sdk.zip

echo 'Extracting the library...'
jar xvf oci-java-sdk.zip lib/oci-java-sdk-full-1.5.2.jar

if [[ "$MAVEN" == "" ]]; then
  export MAVEN="mvn"
fi

echo 'Installing into local Maven repository...'
$MAVEN install:install-file \
       -Dfile=lib/oci-java-sdk-full-1.5.2.jar \
       -DgeneratePom=true \
       -DgroupId=com.oracle.oci \
       -DartifactId=java-sdk \
       -Dversion=1.5.2 \
       -Dpackaging=jar

rm lib/oci-java-sdk-full-1.5.2.jar oci-java-sdk.zip

echo 'Done'