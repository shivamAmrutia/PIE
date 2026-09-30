package com.pie.ingestion;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;

@Component
public class DocumentTextExtractor {

  public String extract(Path path) {
    try (InputStream in = Files.newInputStream(path)) {
      BodyContentHandler handler = new BodyContentHandler(-1);
      Metadata metadata = new Metadata();
      new AutoDetectParser().parse(in, handler, metadata);
      String text = handler.toString();
      return text == null ? "" : text.trim();
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to extract text from " + path.getFileName(), ex);
    }
  }
}
