package com.pie.ingestion;

import com.pie.domain.Document;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class IpsFactExtractor {

  private static final Pattern CONCENTRATION =
      Pattern.compile(
          "maximum allocation to an individual security:\\s*(\\d+(?:\\.\\d+)?)%",
          Pattern.CASE_INSENSITIVE);
  private static final Pattern GOAL_LINE = Pattern.compile("(?m)^-\\s+(.+)$");

  private final JdbcTemplate jdbcTemplate;

  public IpsFactExtractor(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public void extractAndStore(Document document, String text) {
    jdbcTemplate.update(
        "DELETE FROM extracted_facts WHERE source_document_id = ?", document.getId());

    List<Object[]> rows = new ArrayList<>();
    Matcher conc = CONCENTRATION.matcher(text);
    if (conc.find()) {
      rows.add(
          factRow(
              document,
              "IPS_CONCENTRATION_LIMIT",
              "individual_security_pct",
              conc.group(1),
              0.9));
    }

    String goalsBlock = extractSection(text, "Goals");
    if (goalsBlock != null) {
      Matcher goals = GOAL_LINE.matcher(goalsBlock);
      int goalIndex = 0;
      while (goals.find()) {
        rows.add(factRow(document, "GOAL", "goal_" + goalIndex++, goals.group(1).trim(), 0.7));
      }
    }

    for (Object[] row : rows) {
      jdbcTemplate.update(
          """
          INSERT INTO extracted_facts
            (id, client_id, tenant_id, fact_type, fact_key, fact_value, confidence, source_document_id, source_page, source_snippet, as_of_date)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
          """,
          row);
    }
  }

  private String extractSection(String text, String heading) {
    Pattern section =
        Pattern.compile(
            "(?is)##\\s*" + Pattern.quote(heading) + "\\s*(.*?)(?:\\n##\\s*|\\z)");
    Matcher matcher = section.matcher(text);
    return matcher.find() ? matcher.group(1) : null;
  }

  private Object[] factRow(
      Document document, String type, String key, String value, double confidence) {
    return new Object[] {
      UUID.randomUUID(),
      document.getClientId(),
      document.getTenantId(),
      type,
      key,
      value,
      confidence,
      document.getId(),
      1,
      value,
      document.getAsOfDate()
    };
  }
}
