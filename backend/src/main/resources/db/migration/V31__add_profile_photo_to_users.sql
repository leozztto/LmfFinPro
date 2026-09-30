-- Foto de perfil opcional: o arquivo fica no armazenamento de arquivos (chave em photo_key) e o
-- tipo do conteúdo em photo_content_type, reconhecido pelos bytes no upload.
ALTER TABLE users ADD COLUMN photo_key VARCHAR(100);
ALTER TABLE users ADD COLUMN photo_content_type VARCHAR(50);
