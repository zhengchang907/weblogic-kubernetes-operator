#!/bin/bash
# Copyright 2018, Oracle Corporation and/or its affiliates.  All rights reserved.
# Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

# Include common utility functions
source ${CREATE_DOMAIN_SCRIPT_DIR}/utility.sh

export DOMAIN_HOME=${DOMAIN_HOME_DIR}

# Create the domain
wlst.sh -skipWLSModuleScanning ${CREATE_DOMAIN_SCRIPT_DIR}/create-domain.py

# Update the domain for Istio
wlst.sh -skipWLSModuleScanning ${CREATE_DOMAIN_SCRIPT_DIR}/fix-domain.py


