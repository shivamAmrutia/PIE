package com.pie.graph;

import com.pie.domain.Account;
import com.pie.domain.Client;
import com.pie.domain.Document;
import com.pie.domain.Holding;
import java.util.List;
import java.util.UUID;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Service;

@Service
public class ClientGraphSyncService {

  private final Driver driver;

  public ClientGraphSyncService(Driver driver) {
    this.driver = driver;
  }

  public void syncClient(Client client) {
    try (Session session = driver.session()) {
      session.run(
          """
          MERGE (c:Client {id: $id})
          SET c.tenantId = $tenantId, c.displayName = $displayName, c.externalRef = $externalRef
          """,
          Values.parameters(
              "id",
              client.getId().toString(),
              "tenantId",
              client.getTenantId().toString(),
              "displayName",
              client.getDisplayName(),
              "externalRef",
              client.getExternalRef()));
    }
  }

  public void syncAccount(Account account) {
    try (Session session = driver.session()) {
      session.run(
          """
          MERGE (a:Account {id: $id})
          SET a.name = $name, a.accountType = $accountType, a.tenantId = $tenantId
          WITH a
          MATCH (c:Client {id: $clientId})
          MERGE (c)-[:OWNS]->(a)
          """,
          Values.parameters(
              "id",
              account.getId().toString(),
              "name",
              account.getName(),
              "accountType",
              account.getAccountType(),
              "tenantId",
              account.getTenantId().toString(),
              "clientId",
              account.getClientId().toString()));
    }
  }

  public void syncDocument(Document document) {
    try (Session session = driver.session()) {
      session.run(
          """
          MERGE (d:Document {id: $id})
          SET d.filename = $filename, d.docType = $docType, d.tenantId = $tenantId
          WITH d
          MATCH (c:Client {id: $clientId})
          MERGE (d)-[:DESCRIBES]->(c)
          """,
          Values.parameters(
              "id",
              document.getId().toString(),
              "filename",
              document.getFilename(),
              "docType",
              document.getDocType().name(),
              "tenantId",
              document.getTenantId().toString(),
              "clientId",
              document.getClientId().toString()));
    }
  }

  public void syncHoldings(UUID clientId, List<Holding> holdings) {
    try (Session session = driver.session()) {
      session.run(
          "MATCH (c:Client {id: $clientId})-[r:HOLDS]->(:Security) DELETE r",
          Values.parameters("clientId", clientId.toString()));

      for (Holding holding : holdings) {
        session.run(
            """
            MATCH (c:Client {id: $clientId})
            MERGE (s:Security {ticker: $ticker})
            SET s.name = $name, s.assetClass = $assetClass
            MERGE (c)-[r:HOLDS {asOfDate: $asOfDate}]->(s)
            SET r.marketValue = $marketValue, r.accountName = $accountName, r.quantity = $quantity
            """,
            Values.parameters(
                "clientId",
                clientId.toString(),
                "ticker",
                holding.getTicker(),
                "name",
                holding.getSecurityName(),
                "assetClass",
                holding.getAssetClass(),
                "asOfDate",
                holding.getAsOfDate().toString(),
                "marketValue",
                holding.getMarketValue().doubleValue(),
                "accountName",
                holding.getAccountName(),
                "quantity",
                holding.getQuantity().doubleValue()));
      }
    }
  }

  public List<String> relatedTickers(UUID clientId, int limit) {
    try (Session session = driver.session()) {
      return session
          .run(
              """
              MATCH (c:Client {id: $clientId})-[r:HOLDS]->(s:Security)
              RETURN s.ticker AS ticker
              ORDER BY r.marketValue DESC
              LIMIT $limit
              """,
              Values.parameters("clientId", clientId.toString(), "limit", limit))
          .list(record -> record.get("ticker").asString());
    }
  }
}
