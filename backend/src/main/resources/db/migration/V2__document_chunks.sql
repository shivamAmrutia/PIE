-- Document chunks + hybrid retrieval support (pgvector + full-text)

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE document_chunks (
    id              UUID PRIMARY KEY,
    document_id     UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    client_id       UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    chunk_index     INT NOT NULL,
    page_number     INT,
    content         TEXT NOT NULL,
    content_tsv     TSVECTOR,
    embedding       VECTOR(384),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_document_chunk UNIQUE (document_id, chunk_index)
);

CREATE INDEX idx_chunks_client ON document_chunks(client_id);
CREATE INDEX idx_chunks_document ON document_chunks(document_id);
CREATE INDEX idx_chunks_tsv ON document_chunks USING GIN (content_tsv);

CREATE OR REPLACE FUNCTION pie_document_chunks_tsv_trigger() RETURNS trigger AS $$
BEGIN
  NEW.content_tsv := to_tsvector('english', coalesce(NEW.content, ''));
  RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_document_chunks_tsv
BEFORE INSERT OR UPDATE OF content ON document_chunks
FOR EACH ROW EXECUTE FUNCTION pie_document_chunks_tsv_trigger();
