---
title: "Manage operators"
date: 2019-02-23T16:43:38-05:00
weight: 4
description: "Helm is used to create and deploy necessary operator resources and to run the operator in a Kubernetes cluster. Use the operator's Helm chart to install and manage the operator."
---


### Overview

Helm is a framework that helps you manage Kubernetes applications, and Helm charts help you define and install Helm applications into a Kubernetes cluster. The operator's Helm chart is located in the `kubernetes/charts/weblogic-operator` directory.


### Install Helm

Helm manages releases (installations) of your charts. For detailed instructions on installing Helm, see https://github.com/helm/helm.

### Operator's Helm chart configuration

The operator Helm chart is pre-configured with default values for the configuration of the operator.

You can override these values by doing one of the following:

- Creating a custom YAML file with only the values to be overridden, and specifying the `--value` option on the Helm command line.
- Overriding individual values directly on the Helm command line, using the `--set` option.

You can find out the configuration values that the Helm chart supports, as well as the default values, using this command:
```shell
$ helm inspect values kubernetes/charts/weblogic-operator
```

The available configuration values are explained by category in
[Operator Helm configuration values]({{<relref "/userguide/managing-operators/using-helm#operator-helm-configuration-values">}}).

Helm commands are explained in more detail in
[Useful Helm operations]({{<relref "/userguide/managing-operators/using-helm#useful-helm-operations">}}).

#### Optional: Configure the operator's external REST HTTPS interface

The operator can expose an external REST HTTPS interface which can be accessed from outside the Kubernetes cluster. As with the operator's internal REST interface, the external REST interface requires an SSL/TLS certificate and private key that the operator will use as the identity of the external REST interface (see below).

To enable the external REST interface, configure these values in a custom configuration file, or on the Helm command line:

* Set `externalRestEnabled` to `true`.
* Set `externalRestIdentitySecret` to the name of the Kubernetes `tls secret` that contains the certificates and private key.
* Optionally, set `externalRestHttpsPort` to the external port number for the operator REST interface (defaults to `31001`).

For more detailed information, see the [REST interface configuration]({{<relref "/userguide/managing-operators/using-helm#rest-interface-configuration">}}) values.

##### Sample SSL certificate and private key for the REST interface

For testing purposes, the WebLogic Kubernetes Operator project provides a sample script
that generates a self-signed certificate and private key for the operator external REST interface.
The generated certificate and key are stored in a Kubernetes `tls secret` and the sample
script outputs the corresponding configuration values in YAML format. These values can be added to your custom YAML configuration file, for use when the operator's Helm chart is installed.

{{% notice warning %}}
The sample script should ***not*** be used in a production environment because
typically a self-signed certificate for external communication is not considered safe.
A certificate signed by a commercial certificate authority is more widely accepted and
should contain valid host names, expiration dates, and key constraints.
{{% /notice %}}

For more detailed information about the sample script and how to run it, see
the [REST APIs]({{<relref "/samples/rest/_index.md#sample-to-create-certificate-and-key">}}).

#### Optional: Elastic Stack (Elasticsearch, Logstash, and Kibana) integration

The operator Helm chart includes the option of installing the necessary Kubernetes resources for Elastic Stack integration.

You are responsible for configuring Kibana and Elasticsearch, then configuring the operator Helm chart to send events to Elasticsearch. In turn, the operator Helm chart configures Logstash in the operator deployment to send the operator's log contents to that Elasticsearch location.

##### Elastic Stack per-operator configuration

As part of the Elastic Stack integration, Logstash configuration occurs for each deployed operator instance.  You can use the following configuration values to configure the integration:

* Set `elkIntegrationEnabled` is `true` to enable the integration.
* Set `logStashImage` to override the default version of Logstash to be used (`logstash:6.2`).
* Set `elasticSearchHost` and `elasticSearchPort` to override the default location where Elasticsearch is running (`elasticsearch2.default.svc.cluster.local:9201`). This will configure Logstash to send the operator's log contents there.

For more detailed information, see the [Operator Helm configuration values]({{<relref "/userguide/managing-operators/using-helm#operator-helm-configuration-values">}}).
