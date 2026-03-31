-- Limpeza de todas as tabelas em ordem de FK (filhos antes dos pais)
DELETE FROM biometric_data;
DELETE FROM user_achievements;
DELETE FROM train_sessions;
DELETE FROM rankings;
DELETE FROM friendships;
DELETE FROM hordes;
DELETE FROM achievements;
DELETE FROM users;
