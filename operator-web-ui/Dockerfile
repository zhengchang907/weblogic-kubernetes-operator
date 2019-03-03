# Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
# Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
#
# HOW TO BUILD THIS IMAGE
# -----------------------
# Run:
#      $ docker build -t weblogic-kubernetes-operator-web-ui:latest .
#
# Pull base image
# From the Docker store
# -------------------------
FROM node:9.6.1

# Maintainer
# ----------
MAINTAINER Ryan Eberhard <ryan.eberhard@oracle.com>

RUN mkdir /operator
RUN mkdir /operator/public
RUN mkdir /operator/src
ENV PATH=/operator:/operator/node_modules/.bin:$PATH

COPY public /operator/public/
COPY src /operator/src/
COPY operator-web-ui.sh /operator/
COPY livenessProbe.sh /operator/

# install and cache app dependencies
COPY package.json /operator/package.json
RUN cd /operator && \
    npm install && \
    npm install react-scripts@1.1.1 -g

HEALTHCHECK --interval=1m --timeout=10s \
  CMD /operator/livenessProbe.sh

WORKDIR /operator/

CMD ["/operator/operator-web-ui.sh"]
