// Copyright (c) 2019, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package oracle.kubernetes.operator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import oracle.kubernetes.utils.OperatorUtils;
import oracle.kubernetes.weblogic.domain.model.Domain;
import oracle.kubernetes.weblogic.domain.model.DomainCondition;
import oracle.kubernetes.weblogic.domain.model.DomainConditionType;
import oracle.kubernetes.weblogic.domain.model.DomainStatus;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

@SuppressWarnings("unused")
public class DomainConditionMatcher extends TypeSafeDiagnosingMatcher<Domain> {
  private final DomainConditionType expectedType;
  private String expectedStatus;
  private DomainFailureReason expectedReason;
  private String expectedMessage;

  private DomainConditionMatcher(DomainConditionType expectedType) {
    this.expectedType = expectedType;
  }

  public static DomainConditionMatcher hasCondition(DomainConditionType type) {
    return new DomainConditionMatcher(type);
  }

  DomainConditionMatcher withStatus(String status) {
    expectedStatus = status;
    return this;
  }

  public DomainConditionMatcher withReason(DomainFailureReason reason) {
    expectedReason = reason;
    return this;
  }

  public DomainConditionMatcher withMessageContaining(String message) {
    expectedMessage = message;
    return this;
  }

  @Override
  protected boolean matchesSafely(Domain item, Description mismatchDescription) {
    for (DomainCondition condition : getStatus(item).getConditions()) {
      if (matches(condition)) {
        return true;
      }
    }

    if (getStatus(item).getConditions().isEmpty()) {
      mismatchDescription.appendText("found domain with no conditions");
    } else {
      mismatchDescription.appendValueList(
          "found domain with conditions ", ", ", ".", getStatus(item).getConditions());
    }
    return false;
  }

  private boolean matches(DomainCondition condition) {
    if (expectedType != condition.getType()) {
      return false;
    }
    if (expectedStatus != null && !expectedStatus.equals(condition.getStatus())) {
      return false;
    }
    if (expectedMessage != null && !messageContainsExpectedString(condition)) {
      return false;
    }
    return expectedReason == null || expectedReason.toString().equals(condition.getReason());
  }

  private boolean messageContainsExpectedString(DomainCondition condition) {
    return condition.getMessage() != null && condition.getMessage().contains(expectedMessage);
  }

  private DomainStatus getStatus(Domain domain) {
    return Optional.ofNullable(domain.getStatus()).orElse(new DomainStatus());
  }

  @Override
  public void describeTo(Description description) {
    List<String> expectations = new ArrayList<>();
    expectations.add(expectation("type", expectedType.toString()));
    if (expectedStatus != null) {
      expectations.add(expectation("status", expectedStatus));
    }
    if (expectedReason != null) {
      expectations.add(expectation("reason", expectedReason.toString()));
    }
    if (expectedMessage != null) {
      expectations.add(expectation("message containing", expectedMessage));
    }
    description
        .appendText("domain containing condition: ")
        .appendText(OperatorUtils.joinListGrammatically(expectations));
  }

  private String expectation(String description, String value) {
    return description + " = '" + value + "'";
  }
}
