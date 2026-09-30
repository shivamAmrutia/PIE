package com.pie.repository;

import com.pie.domain.DocumentChunk;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class DocumentChunkRepository {

  private static final RowMapper<RetrievedChunk> RETRIEVED_MAPPER =
      (rs, rowNum) ->
          new RetrievedChunk(
              mapChunk(rs),
              rs.getString("filename"),
              rs.getString("doc_type"),
              rs.getDouble("score"));

  private final JdbcTemplate jdbcTemplate;

  public DocumentChunkRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public void deleteByDocumentId(UUID documentId) {
    jdbcTemplate.update("DELETE FROM document_chunks WHERE document_id = ?", documentId);
  }

  public void insert(DocumentChunk chunk, float[] embedding) {
    String vectorLiteral = toVectorLiteral(embedding);
    jdbcTemplate.update(
        connection -> {
          PreparedStatement ps =
              connection.prepareStatement(
                  """
                  INSERT INTO document_chunks
                    (id, document_id, client_id, tenant_id, chunk_index, page_number, content, embedding)
                  VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS vector))
                  """);
          ps.setObject(1, chunk.getId());
          ps.setObject(2, chunk.getDocumentId());
          ps.setObject(3, chunk.getClientId());
          ps.setObject(4, chunk.getTenantId());
          ps.setInt(5, chunk.getChunkIndex());
          if (chunk.getPageNumber() == null) {
            ps.setObject(6, null);
          } else {
            ps.setInt(6, chunk.getPageNumber());
          }
          ps.setString(7, chunk.getContent());
          ps.setString(8, vectorLiteral);
          return ps;
        });
  }

  public List<RetrievedChunk> keywordSearch(UUID clientId, String query, int limit) {
    return jdbcTemplate.query(
        """
        SELECT c.*, d.filename, d.doc_type,
               ts_rank(c.content_tsv, plainto_tsquery('english', ?)) AS score
        FROM document_chunks c
        JOIN documents d ON d.id = c.document_id
        WHERE c.client_id = ?
          AND c.content_tsv @@ plainto_tsquery('english', ?)
        ORDER BY score DESC
        LIMIT ?
        """,
        RETRIEVED_MAPPER,
        query,
        clientId,
        query,
        limit);
  }

  public List<RetrievedChunk> semanticSearch(UUID clientId, float[] embedding, int limit) {
    String vectorLiteral = toVectorLiteral(embedding);
    return jdbcTemplate.query(
        """
        SELECT c.*, d.filename, d.doc_type,
               1 - (c.embedding <=> CAST(? AS vector)) AS score
        FROM document_chunks c
        JOIN documents d ON d.id = c.document_id
        WHERE c.client_id = ?
          AND c.embedding IS NOT NULL
        ORDER BY c.embedding <=> CAST(? AS vector)
        LIMIT ?
        """,
        RETRIEVED_MAPPER,
        vectorLiteral,
        clientId,
        vectorLiteral,
        limit);
  }

  public List<DocumentChunk> findByClientId(UUID clientId, int limit) {
    return jdbcTemplate.query(
        """
        SELECT * FROM document_chunks
        WHERE client_id = ?
        ORDER BY created_at DESC
        LIMIT ?
        """,
        (rs, rowNum) -> mapChunk(rs),
        clientId,
        limit);
  }

  private static DocumentChunk mapChunk(ResultSet rs) throws SQLException {
    return new DocumentChunk(
        (UUID) rs.getObject("id"),
        (UUID) rs.getObject("document_id"),
        (UUID) rs.getObject("client_id"),
        (UUID) rs.getObject("tenant_id"),
        rs.getInt("chunk_index"),
        (Integer) rs.getObject("page_number"),
        rs.getString("content"),
        rs.getTimestamp("created_at").toInstant());
  }

  private static String toVectorLiteral(float[] embedding) {
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < embedding.length; i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append(embedding[i]);
    }
    sb.append(']');
    return sb.toString();
  }

  public record RetrievedChunk(
      DocumentChunk chunk, String filename, String docType, double score) {}
}
