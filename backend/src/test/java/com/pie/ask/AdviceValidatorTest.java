package com.pie.ask;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AdviceValidatorTest {

  private final AdviceValidator validator = new AdviceValidator();

  @Test
  void allowsFactualAnswer() {
    var result = validator.validate("XYZ is 17.8% of the taxable portfolio.");
    assertFalse(result.rewritten());
  }

  @Test
  void blocksRecommendationLanguage() {
    var result = validator.validate("You should sell XYZ shares immediately.");
    assertTrue(result.rewritten());
    assertFalse(result.answer().toLowerCase().contains("you should sell"));
  }
}
