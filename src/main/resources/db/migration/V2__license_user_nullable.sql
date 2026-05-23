-- user_id заполняется при первой активации (ER / методичка)
ALTER TABLE license ALTER COLUMN user_id DROP NOT NULL;
