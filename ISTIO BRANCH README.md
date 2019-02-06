# Istio Branch

This branch contains changes made to the WebLogic Operator to get it to the point where it was able to successfully start a domain in an Istio environment.

The wiki [Summary of Exploring how to run WebLogic and Operator on Istio](http://aseng-wiki.us.oracle.com/asengwiki/pages/viewpage.action?pageId=5251717305) contains a summary of work we did for [OWLS-71725](https://jira.oraclecorp.com/jira/browse/OWLS-71725)

## Building the Operator

The changes being made are not compatible with the unit tests.  Use the following command to build without running tests:

`mvn clean install -Dmaven.test.skip=true`

