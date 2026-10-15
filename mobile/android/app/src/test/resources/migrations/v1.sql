CREATE TABLE documents (id TEXT PRIMARY KEY, source_uri TEXT NOT NULL UNIQUE, filename TEXT NOT NULL UNIQUE, title TEXT NOT NULL, fingerprint TEXT NOT NULL, size_bytes INTEGER NOT NULL, page_count INTEGER NOT NULL CHECK(page_count > 0), created_at INTEGER NOT NULL);
CREATE TABLE reading_positions (document_id TEXT PRIMARY KEY REFERENCES documents(id) ON DELETE CASCADE, page_index INTEGER NOT NULL CHECK(page_index >= 0), updated_at INTEGER NOT NULL);
CREATE INDEX positions_recent ON reading_positions(updated_at DESC);
INSERT INTO documents VALUES('old-book','content://old-provider/book','old.pdf','Eski kitap','hash',3,500,1);
INSERT INTO reading_positions VALUES('old-book',41,2);
