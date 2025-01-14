// Copyright (c) 2018, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package oracle.kubernetes.operator.helm;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

@SuppressWarnings("SameParameterValue")
class OperatorChartIt extends OperatorChartItBase {

  private static final InstallArgs NO_VALUES_INSTALL_ARGS = newInstallArgs(Collections.emptyMap());

  @Test
  void whenChartsGenerated_haveTwoRoleBindings() throws Exception {
    ProcessedChart chart = getChart(NO_VALUES_INSTALL_ARGS);

    assertThat(chart.getDocuments("RoleBinding"), hasSize(2));
  }
}
