// Copyright (c) 2018, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package oracle.kubernetes.operator.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.WebApplicationException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.meterware.simplestub.Memento;
import com.meterware.simplestub.StaticStubSupport;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1SubjectAccessReview;
import io.kubernetes.client.openapi.models.V1SubjectAccessReviewStatus;
import io.kubernetes.client.openapi.models.V1TokenReview;
import io.kubernetes.client.openapi.models.V1TokenReviewStatus;
import io.kubernetes.client.openapi.models.V1UserInfo;
import oracle.kubernetes.operator.DomainProcessingTestBase;
import oracle.kubernetes.operator.DomainProcessorImpl;
import oracle.kubernetes.operator.DomainProcessorTestSetup;
import oracle.kubernetes.operator.helpers.DomainPresenceInfo;
import oracle.kubernetes.operator.helpers.KubernetesTestSupport;
import oracle.kubernetes.operator.rest.RestBackendImpl.TopologyRetriever;
import oracle.kubernetes.operator.rest.backend.RestBackend;
import oracle.kubernetes.operator.rest.model.DomainUpdate;
import oracle.kubernetes.operator.rest.model.DomainUpdateType;
import oracle.kubernetes.operator.utils.WlsDomainConfigSupport;
import oracle.kubernetes.operator.wlsconfig.WlsDomainConfig;
import oracle.kubernetes.utils.TestUtils;
import oracle.kubernetes.weblogic.domain.ClusterConfigurator;
import oracle.kubernetes.weblogic.domain.DomainConfigurator;
import oracle.kubernetes.weblogic.domain.DomainConfiguratorFactory;
import oracle.kubernetes.weblogic.domain.model.Domain;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static oracle.kubernetes.operator.DomainProcessorTestSetup.NS;
import static oracle.kubernetes.operator.DomainProcessorTestSetup.UID;
import static oracle.kubernetes.operator.DomainProcessorTestSetup.createTestDomain;
import static oracle.kubernetes.operator.helpers.KubernetesTestSupport.DOMAIN;
import static oracle.kubernetes.operator.helpers.KubernetesTestSupport.SUBJECT_ACCESS_REVIEW;
import static oracle.kubernetes.operator.helpers.KubernetesTestSupport.TOKEN_REVIEW;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.junit.MatcherAssert.assertThat;

@SuppressWarnings("SameParameterValue")
public class RestBackendImplTest extends DomainProcessingTestBase {

  private static final int REPLICA_LIMIT = 4;
  private static final String UID2 = "domain2";
  public static final String ADMIN = "admin";
  private final WlsDomainConfigSupport configSupport = new WlsDomainConfigSupport(UID);

  private final List<Memento> mementos = new ArrayList<>();
  private RestBackend restBackend;
  private final Domain domain1 = createTestDomain();
  private final Domain domain2 = createTestDomain();
  private Domain updatedDomain;
  private final DomainConfigurator configurator = DomainConfiguratorFactory.forDomain(domain1);
  private WlsDomainConfig config;

  /**
   * Setup test.
   * @throws Exception on failure
   */
  @Before
  public void setUp() throws Exception {
    super.setUp();
    mementos.add(TestUtils.silenceOperatorLogger());
    mementos.add(
        StaticStubSupport.install(RestBackendImpl.class, "INSTANCE", new TopologyRetrieverStub()));

    renameDomain(domain2, UID2);
    testSupport.defineResources(domain1, domain2);
    testSupport.doOnCreate(TOKEN_REVIEW, r -> authenticate((V1TokenReview) r));
    testSupport.doOnCreate(SUBJECT_ACCESS_REVIEW, s -> allow((V1SubjectAccessReview) s));
    testSupport.doOnUpdate(DOMAIN, d -> updatedDomain = (Domain) d);
    configSupport.addWlsCluster("cluster1", "ms1", "ms2", "ms3", "ms4", "ms5", "ms6");
    restBackend = new RestBackendImpl(processor, "", "", Collections.singletonList(NS));

    setupScanCache();
  }

  private void renameDomain(Domain domain, String name) {
    domain.getMetadata().setName(name);
    domain.getSpec().setDomainUid(name);
  }

  private void authenticate(V1TokenReview tokenReview) {
    tokenReview.setStatus(new V1TokenReviewStatus().authenticated(true).user(new V1UserInfo()));
  }

  private void allow(V1SubjectAccessReview subjectAccessReview) {
    subjectAccessReview.setStatus(new V1SubjectAccessReviewStatus().allowed(true));
  }

  @After
  public void tearDown() {
    super.tearDown();
    mementos.forEach(Memento::revert);
  }

  @Test(expected = WebApplicationException.class)
  public void whenUnknownDomain_throwException() {
    restBackend.updateDomain("no_such_uid", new DomainUpdate(DomainUpdateType.INTROSPECT));
  }

  @Test(expected = WebApplicationException.class)
  public void whenUnknownDomainUpdateCommand_throwException() {
    restBackend.updateDomain(UID, new DomainUpdate(null));
  }

  @Test
  public void whenIntrospectionRequested_runIntrospectionJob() throws JsonProcessingException {
    new DomainProcessorTestSetup(testSupport).defineKubernetesResources(createDomainConfig());
    defineServerResources(ADMIN);
    defineConfigurationOverridesMap();
    DomainProcessorImpl.registerDomainPresenceInfo(new DomainPresenceInfo(domain1));
    testSupport.doOnCreate(KubernetesTestSupport.JOB, j -> recordJob((V1Job) j));

    restBackend.updateDomain(UID, new DomainUpdate(DomainUpdateType.INTROSPECT));

    assertThat(job, notNullValue());
  }

  private V1Job job;

  private void recordJob(V1Job job) {
    this.job = job;
  }

  private WlsDomainConfig createDomainConfig() {
    return new WlsDomainConfig("base_domain")
        .withAdminServer(ADMIN, "domain1-admin-server", 7001);
  }

  @Test(expected = WebApplicationException.class)
  public void whenNegativeScaleSpecified_throwException() {
    restBackend.scaleCluster(UID, "cluster1", -1);
  }

  @Test
  public void whenPerClusterReplicaSettingMatchesScaleRequest_doNothing() {
    configureCluster("cluster1").withReplicas(5);

    restBackend.scaleCluster(UID, "cluster1", 5);

    assertThat(getUpdatedDomain(), nullValue());
  }

  private Domain getUpdatedDomain() {
    return updatedDomain;
  }

  private ClusterConfigurator configureCluster(String clusterName) {
    return configureDomain().configureCluster(clusterName);
  }

  @Test
  public void whenPerClusterReplicaSetting_scaleClusterUpdatesSetting() {
    configureCluster("cluster1").withReplicas(1);

    restBackend.scaleCluster(UID, "cluster1", 5);

    assertThat(getUpdatedDomain().getReplicaCount("cluster1"), equalTo(5));
  }

  @Test
  @Ignore
  public void whenNoPerClusterReplicaSetting_scaleClusterCreatesOne() {
    restBackend.scaleCluster(UID, "cluster1", 5);

    assertThat(getUpdatedDomain().getReplicaCount("cluster1"), equalTo(5));
  }

  @Test
  public void whenNoPerClusterReplicaSettingAndDefaultMatchesRequest_doNothing() {
    configureDomain().withDefaultReplicaCount(REPLICA_LIMIT);

    restBackend.scaleCluster(UID, "cluster1", REPLICA_LIMIT);

    assertThat(getUpdatedDomain(), nullValue());
  }

  @Test(expected = WebApplicationException.class)
  public void whenReplaceDomainReturnsError_scaleClusterThrowsException() {
    testSupport.failOnResource(DOMAIN, UID2, NS, HTTP_CONFLICT);

    DomainConfiguratorFactory.forDomain(domain2).configureCluster("cluster1").withReplicas(2);

    restBackend.scaleCluster(UID2, "cluster1", 3);
  }

  @Test
  public void verify_getWlsDomainConfig_returnsWlsDomainConfig() {
    WlsDomainConfig wlsDomainConfig = ((RestBackendImpl) restBackend).getWlsDomainConfig(UID);

    assertThat(wlsDomainConfig.getName(), equalTo(UID));
  }

  @Test
  public void verify_getWlsDomainConfig_doesNotReturnNull_whenNoSuchDomainUid() {
    WlsDomainConfig wlsDomainConfig =
        ((RestBackendImpl) restBackend).getWlsDomainConfig("NoSuchDomainUID");

    assertThat(wlsDomainConfig, notNullValue());
  }

  @Test
  public void verify_getWlsDomainConfig_doesNotReturnNull_whenScanIsNull() {
    config = null;

    WlsDomainConfig wlsDomainConfig = ((RestBackendImpl) restBackend).getWlsDomainConfig(UID);

    assertThat(wlsDomainConfig, notNullValue());
  }

  private DomainConfigurator configureDomain() {
    return configurator;
  }

  private void setupScanCache() {
    config = configSupport.createDomainConfig();
  }

  private class TopologyRetrieverStub implements TopologyRetriever {
    @Override
    public WlsDomainConfig getWlsDomainConfig(String ns, String domainUid) {
      return config;
    }
  }
}
