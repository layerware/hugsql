-- queries.sql
-- Test Functions

-- :name create-colors-table
-- :command :execute
-- :result :raw
-- :doc Create a test table of colors
CREATE TABLE IF NOT EXISTS colors
(
  id INTEGER AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(32),
  r INTEGER,
  g INTEGER,
  b INTEGER
);

-- :name insert-color
-- :command :execute
-- :result :raw
-- :doc Insert a color into the colors table
INSERT INTO colors (name, r, g, b)
VALUES (:name, :r, :g, :b);

-- :name insert-color-alt
-- :command :insert
-- :result :raw
-- :doc Insert a color into the colors table returning keys
INSERT INTO colors (name, r, g, b)
VALUES (:name, :r, :g, :b);

-- :name update-color-affected :! :n
UPDATE colors
SET r = :r;

-- :name delete-color-by-id
-- :command :execute
-- :result :raw
-- :doc Delete a color by the given `id`
DELETE FROM colors WHERE id = :id;

-- :name select-color-by-id :? :1
-- :command :query
-- :doc Select a color by id
SELECT * FROM colors WHERE id = :id LIMIT 1;

-- :name select-all-colors :? :*
-- :command :query
-- :doc Select all colors
SELECT * FROM colors;