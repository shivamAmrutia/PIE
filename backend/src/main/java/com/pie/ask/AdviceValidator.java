package com.pie.ask;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class AdviceValidator {

  private static final List<Pattern> BLOCKED =
      List.of(
          Pattern.compile("\\b(you should|i recommend|we recommend)\\b", Pattern.CASE_INSENSITIVE),
          Pattern.compile(
              "\\b(buy|sell|trim|add to)\\b.{0,40}\\b(shares|position|holding)\\b",
              Pattern.CASE_INSENSITIVE),
          Pattern.compile("\\b(allocate|rebalance)\\b.{0,40}\\b(to|into)\\b", Pattern.CASE_INSENSITIVE));

  public ValidationResult validate(String answer) {
    if (answer == null || answer.isBlank()) {
      return ValidationResult.ok(answer);
    }
    String cleaned = answer;
    boolean rewritten = false;
    for (Pattern pattern : BLOCKED) {
      if (pattern.matcher(cleaned).find()) {
        rewritten = true;
        break;
      }
    }
    if (!rewritten) {
      return ValidationResult.ok(cleaned);
    }
    String safe =
        "PIE cannot provide investment recommendations or trade instructions. "
            + "Based on available evidence, here are facts and calculations for advisor review:\n\n"
            + stripAdviceLanguage(cleaned);
    return new ValidationResult(safe, true, "Removed or rewrote advice-like language");
  }

  private String stripAdviceLanguage(String text) {
    return text.replaceAll("(?i)\\byou should\\b[^\\.]*\\.", "")
        .replaceAll("(?i)\\bi recommend\\b[^\\.]*\\.", "")
        .replaceAll("(?i)\\bwe recommend\\b[^\\.]*\\.", "")
        .trim();
  }

  public record ValidationResult(String answer, boolean rewritten, String note) {
    static ValidationResult ok(String answer) {
      return new ValidationResult(answer, false, null);
    }
  }
}
