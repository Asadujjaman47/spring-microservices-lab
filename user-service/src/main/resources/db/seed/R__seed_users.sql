-- Repeatable (idempotent) seed. BCrypt hashes below are for the password "Demo@1234".
INSERT INTO users (id, email, password_hash, first_name, last_name, created_at, updated_at, created_by)
VALUES
  ('00000000-0000-7000-8000-000000000001',
   'ada@example.com',
   '$2a$10$TnR24j/8I7/0k645N9cp9u3aAolTDWF/ZzgP0bY7YsaDRpez4XVwy',
   'Ada', 'Lovelace', now(), now(), 'seed'),
  ('00000000-0000-7000-8000-000000000002',
   'alan@example.com',
   '$2a$10$TnR24j/8I7/0k645N9cp9u3aAolTDWF/ZzgP0bY7YsaDRpez4XVwy',
   'Alan', 'Turing', now(), now(), 'seed')
ON CONFLICT (email) DO NOTHING;