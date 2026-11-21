-- V13: Tim kiem ten san pham khong phan biet dau (FR-PROD-05): go "ao thun" ra "Áo Thun".
-- unaccent co san trong postgres contrib (image postgres:16-alpine). Chi them extension va ham,
-- khong sua bang hay du lieu.
CREATE EXTENSION IF NOT EXISTS unaccent;

-- unaccent() chi la STABLE; boc lai thanh IMMUTABLE (co dinh tu dien) de sau nay danh index duoc.
CREATE OR REPLACE FUNCTION f_unaccent(text) RETURNS text
    LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT
AS $$ SELECT public.unaccent('public.unaccent'::regdictionary, $1) $$;
