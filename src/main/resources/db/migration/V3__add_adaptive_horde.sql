-- V3: Suporte a horda adaptativa
-- Permite que uma horda ajuste seu pace dinamicamente à performance média dos usuários.
ALTER TABLE hordes ADD COLUMN is_adaptive BOOLEAN NOT NULL DEFAULT FALSE;
