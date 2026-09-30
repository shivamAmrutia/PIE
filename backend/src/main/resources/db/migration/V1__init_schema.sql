-- PIE schema: tenants, users, clients, authz, audit, documents, holdings

CREATE TABLE tenants (
    id              UUID PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE users (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    email           VARCHAR(320) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(255) NOT NULL,
    role            VARCHAR(32) NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_tenant_email UNIQUE (tenant_id, email)
);

CREATE INDEX idx_users_tenant ON users(tenant_id);

CREATE TABLE clients (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    display_name    VARCHAR(255) NOT NULL,
    external_ref    VARCHAR(64),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_clients_tenant ON clients(tenant_id);

CREATE TABLE client_access (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    client_id       UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    access_level    VARCHAR(32) NOT NULL DEFAULT 'READ_WRITE',
    granted_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_client_access UNIQUE (user_id, client_id)
);

CREATE INDEX idx_client_access_user ON client_access(user_id);
CREATE INDEX idx_client_access_client ON client_access(client_id);

CREATE TABLE accounts (
    id              UUID PRIMARY KEY,
    client_id       UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    name            VARCHAR(255) NOT NULL,
    account_type    VARCHAR(64) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_accounts_client ON accounts(client_id);

CREATE TABLE documents (
    id              UUID PRIMARY KEY,
    client_id       UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    filename        VARCHAR(512) NOT NULL,
    content_type    VARCHAR(128) NOT NULL,
    doc_type        VARCHAR(64) NOT NULL DEFAULT 'OTHER',
    storage_path    VARCHAR(1024) NOT NULL,
    page_count      INT,
    as_of_date      DATE,
    uploaded_by     UUID REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_documents_client ON documents(client_id);

CREATE TABLE holdings (
    id              UUID PRIMARY KEY,
    client_id       UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    account_id      UUID REFERENCES accounts(id) ON DELETE SET NULL,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    account_name    VARCHAR(255) NOT NULL,
    ticker          VARCHAR(64) NOT NULL,
    security_name   VARCHAR(255),
    quantity        NUMERIC(20, 6) NOT NULL,
    price           NUMERIC(20, 6) NOT NULL,
    market_value    NUMERIC(20, 2) NOT NULL,
    asset_class     VARCHAR(64),
    as_of_date      DATE NOT NULL,
    source_document_id UUID REFERENCES documents(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_holdings_client_asof ON holdings(client_id, as_of_date);

CREATE TABLE extracted_facts (
    id              UUID PRIMARY KEY,
    client_id       UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    fact_type       VARCHAR(64) NOT NULL,
    fact_key        VARCHAR(255) NOT NULL,
    fact_value      TEXT NOT NULL,
    confidence      NUMERIC(5, 4),
    source_document_id UUID REFERENCES documents(id),
    source_page     INT,
    source_snippet  TEXT,
    as_of_date      DATE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_facts_client ON extracted_facts(client_id);

CREATE TABLE audit_events (
    id              UUID PRIMARY KEY,
    tenant_id       UUID REFERENCES tenants(id),
    user_id         UUID REFERENCES users(id),
    client_id       UUID,
    action          VARCHAR(64) NOT NULL,
    resource_type   VARCHAR(64),
    resource_id     VARCHAR(128),
    detail          TEXT,
    ip_address      VARCHAR(64),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_tenant_created ON audit_events(tenant_id, created_at DESC);
CREATE INDEX idx_audit_user_created ON audit_events(user_id, created_at DESC);
