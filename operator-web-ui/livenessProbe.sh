#!/bin/bash
# Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
# Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

# check that the node process is running
TEST=`ps | grep node | wc -l | awk ' { print $1; } '`

# if it is not, then send liveness failre (i.e. non-zero exit code)
if [ "$TEST" != "0" ]; then
    exit 0
else
    exit 1
fi



