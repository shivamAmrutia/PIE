package com.pie.ingestion;

import com.pie.domain.Document;
import com.pie.domain.DocumentChunk;
import com.pie.domain.DocumentType;
import com.pie.embedding.EmbeddingService;
import com.pie.graph.ClientGraphSyncService;
import com.pie.repository.DocumentChunkRepository;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentIngestionService {

  private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

  private final DocumentTextExtractor textExtractor;
  private final TextChunker textChunker;
  private final EmbeddingService embeddingService;
  private final DocumentChunkRepository chunkRepository;
  private final ClientGraphSyncService graphSyncService;
  private final IpsFactExtractor ipsFactExtractor;

  public DocumentIngestionService(
      DocumentTextExtractor textExtractor,
      TextChunker textChunker,
      EmbeddingService embeddingService,
      DocumentChunkRepository chunkRepository,
      ClientGraphSyncService graphSyncService,
      IpsFactExtractor ipsFactExtractor) {
    this.textExtractor = textExtractor;
    this.textChunker = textChunker;
    this.embeddingService = embeddingService;
    this.chunkRepository = chunkRepository;
    this.graphSyncService = graphSyncService;
    this.ipsFactExtractor = ipsFactExtractor;
  }

  @Transactional
  public int ingest(Document document) {
    Path path = Path.of(document.getStoragePath());
    String text = textExtractor.extract(path);
    chunkRepository.deleteByDocumentId(document.getId());

    List<String> pieces = textChunker.chunk(text);
    int index = 0;
    for (String piece : pieces) {
      DocumentChunk chunk =
          new DocumentChunk(
              UUID.randomUUID(),
              document.getId(),
              document.getClientId(),
              document.getTenantId(),
              index,
              guessPage(index),
              piece,
              Instant.now());
      chunkRepository.insert(chunk, embeddingService.embed(piece));
      index++;
    }

    if (document.getDocType() == DocumentType.IPS || document.getFilename().toLowerCase().contains("ips")) {
      ipsFactExtractor.extractAndStore(document, text);
    }

    graphSyncService.syncDocument(document);
    log.info("Ingested document {} into {} chunks", document.getFilename(), pieces.size());
    return pieces.size();
  }

  private Integer guessPage(int chunkIndex) {
    return chunkIndex + 1;
  }
}
