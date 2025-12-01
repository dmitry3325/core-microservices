-- Migration: Add document indexes and audit fields
-- Date: 2025-12-01
-- Description: Adds performance indexes for documents table and creates document_access_tokens table

-- Add audit fields to documents table if they don't exist
ALTER TABLE documents ADD COLUMN IF NOT EXISTS deleted_by UUID;
ALTER TABLE documents ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

-- Add indexes on documents table for performance
CREATE INDEX IF NOT EXISTS idx_document_uploaded_by_id ON documents(uploaded_by_id);
CREATE INDEX IF NOT EXISTS idx_document_deleted ON documents(deleted);
CREATE INDEX IF NOT EXISTS idx_document_visibility ON documents(visibility);
CREATE INDEX IF NOT EXISTS idx_document_created_at ON documents(created_at);
CREATE INDEX IF NOT EXISTS idx_document_uuid ON documents(uuid);

-- Create document_access_tokens table
CREATE TABLE IF NOT EXISTS document_access_tokens (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    document_uuid UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT false,
    revoked_by UUID,
    revoked_at TIMESTAMP,
    access_count INTEGER NOT NULL DEFAULT 0,
    last_accessed_at TIMESTAMP
);

-- Add indexes on document_access_tokens table
CREATE INDEX IF NOT EXISTS idx_token_hash ON document_access_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_token_document_uuid ON document_access_tokens(document_uuid);
CREATE INDEX IF NOT EXISTS idx_token_expires_at ON document_access_tokens(expires_at);
CREATE INDEX IF NOT EXISTS idx_token_revoked ON document_access_tokens(revoked);

-- Add foreign key constraint (optional, can be added if documents table has uuid as unique key)
-- ALTER TABLE document_access_tokens
-- ADD CONSTRAINT fk_token_document
-- FOREIGN KEY (document_uuid) REFERENCES documents(uuid) ON DELETE CASCADE;

-- Comments for documentation
COMMENT ON TABLE document_access_tokens IS 'Stores JWT tokens for BY_LINK document access with validation and audit trail';
COMMENT ON COLUMN document_access_tokens.token_hash IS 'SHA-256 hash of the JWT token for lookup and validation';
COMMENT ON COLUMN document_access_tokens.access_count IS 'Number of times this token has been used to access the document';
COMMENT ON COLUMN documents.deleted_by IS 'User ID who soft-deleted this document';
COMMENT ON COLUMN documents.deleted_at IS 'Timestamp when the document was soft-deleted';

