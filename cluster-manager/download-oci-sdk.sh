#!/bin/sh

echo 'Downloading the OCI Java SDK...'
wget -q https://github.com/oracle/oci-java-sdk/releases/download/v1.3.7/oci-java-sdk.zip

jar xvf oci-java-sdk.zip lib/oci-java-sdk-full-1.3.7.jar

mvn install:install-file \
    -Dfile=lib/oci-java-sdk-full-1.3.7.jar \
    -DgeneratePom=true \
    -DgroupId=com.oracle.oci \
    -DartifactId=java-sdk \
    -Dversion=1.3.7 \
    -Dpackaging=jar

rm lib/oci-java-sdk-full-1.3.7.jar oci-java-sdk.zip