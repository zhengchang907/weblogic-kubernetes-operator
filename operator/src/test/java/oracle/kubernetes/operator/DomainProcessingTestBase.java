// Copyright (c) 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package oracle.kubernetes.operator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.meterware.simplestub.Memento;
import com.meterware.simplestub.StaticStubSupport;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1Service;
import oracle.kubernetes.operator.helpers.AnnotationHelper;
import oracle.kubernetes.operator.helpers.ConfigMapHelper;
import oracle.kubernetes.operator.helpers.DomainPresenceInfo;
import oracle.kubernetes.operator.helpers.DomainTopology;
import oracle.kubernetes.operator.helpers.KubernetesTestSupport;
import oracle.kubernetes.operator.helpers.KubernetesUtils;
import oracle.kubernetes.operator.helpers.LegalNames;
import oracle.kubernetes.operator.helpers.TuningParametersStub;
import oracle.kubernetes.operator.helpers.UnitTestHash;
import oracle.kubernetes.operator.rest.ScanCacheStub;
import oracle.kubernetes.operator.utils.InMemoryCertificates;
import oracle.kubernetes.operator.utils.WlsDomainConfigSupport;
import org.junit.After;
import org.junit.Before;

import static oracle.kubernetes.operator.DomainProcessorTestSetup.NS;
import static oracle.kubernetes.operator.DomainProcessorTestSetup.UID;
import static oracle.kubernetes.operator.LabelConstants.SERVERNAME_LABEL;

public class DomainProcessingTestBase {

  protected final KubernetesTestSupport testSupport = new KubernetesTestSupport();
  protected final DomainProcessorImpl processor =
      new DomainProcessorImpl(DomainProcessorDelegateStub.createDelegate(testSupport));
  private final Map<String, Map<String, DomainPresenceInfo>> presenceInfoMap = new HashMap<>();
  private final List<Memento> mementos = new ArrayList<>();

  @Before
  public void setUp() throws Exception {
    mementos.add(testSupport.install());
    mementos.add(StaticStubSupport.install(DomainProcessorImpl.class, "DOMAINS", presenceInfoMap));
    mementos.add(TuningParametersStub.install());
    mementos.add(InMemoryCertificates.install());
    mementos.add(UnitTestHash.install());
    mementos.add(ScanCacheStub.install());
    
    DomainProcessorTestSetup.defineRequiredResources(testSupport);
  }

  @After
  public void tearDown() {
    mementos.forEach(Memento::revert);
  }

  protected void defineServerResources(String serverName) {
    testSupport.defineResources(createServerPod(serverName), createServerService(serverName));
  }

  private V1Pod createServerPod(String serverName) {
    return AnnotationHelper.withSha256Hash(
        new V1Pod()
            .metadata(
                withServerLabels(
                    new V1ObjectMeta()
                        .name(LegalNames.toPodName(DomainProcessorTestSetup.UID, serverName))
                        .namespace(NS),
                    serverName))
            .spec(new V1PodSpec()));
  }

  private V1ObjectMeta withServerLabels(V1ObjectMeta meta, String serverName) {
    return KubernetesUtils.withOperatorLabels(DomainProcessorTestSetup.UID, meta)
        .putLabelsItem(SERVERNAME_LABEL, serverName);
  }

  private V1Service createServerService(String serverName) {
    return AnnotationHelper.withSha256Hash(
        new V1Service()
            .metadata(
                withServerLabels(
                    new V1ObjectMeta()
                        .name(
                            LegalNames.toServerServiceName(
                                DomainProcessorTestSetup.UID, serverName))
                        .namespace(NS),
                    serverName)));
  }

  protected void defineConfigurationOverridesMap() throws JsonProcessingException {
    testSupport.defineResources(createConfigurationOverridesMap());
  }

  // define a config map with a topology to avoid the no-topology condition that always runs the introspector
  private V1ConfigMap createConfigurationOverridesMap() throws JsonProcessingException {
    return new V1ConfigMap()
          .metadata(createConfigurationOverridesMapMeta())
          .data(new HashMap<>(Map.of(IntrospectorConfigMapKeys.TOPOLOGY_YAML, defineTopology())));
  }

  private V1ObjectMeta createConfigurationOverridesMapMeta() {
    return new V1ObjectMeta().namespace(NS).name(ConfigMapHelper.getIntrospectorConfigMapName(UID));
  }

  private String defineTopology() throws JsonProcessingException {
    WlsDomainConfigSupport configSupport = new WlsDomainConfigSupport("domain")
          .withAdminServerName("admin").withWlsServer("admin", 8045);

    return new ObjectMapper(new YAMLFactory())
          .setSerializationInclusion(JsonInclude.Include.NON_DEFAULT)
          .writeValueAsString(new DomainTopology(configSupport.createDomainConfig()));
  }

}
