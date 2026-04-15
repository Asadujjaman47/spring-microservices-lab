-- Repeatable (idempotent) seed — safe to re-run on every Flyway cycle.
INSERT INTO products (id, name, description, price_cents, stock, created_at, updated_at, created_by)
VALUES
  ('00000000-0000-7000-8000-00000000aa01',
   'Notebook',
   'A5 dotted notebook, 160 pages.',
   1299, 50, now(), now(), 'seed'),
  ('00000000-0000-7000-8000-00000000aa02',
   'Mechanical keyboard',
   '75% layout, hot-swappable switches.',
   12999, 15, now(), now(), 'seed'),
  ('00000000-0000-7000-8000-00000000aa03',
   'Coffee beans',
   'Single-origin, medium roast, 250g.',
   1599, 100, now(), now(), 'seed')
ON CONFLICT (id) DO NOTHING;