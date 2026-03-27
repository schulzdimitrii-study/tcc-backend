-- =========================================================
-- V2: Criação das tabelas de domínio do Bio Survival
-- Ordem respeitando dependências de FK
-- =========================================================

-- 1. Achievements (sem FK)
CREATE TABLE achievements (
    id          UUID          NOT NULL,
    title       VARCHAR(255)  NOT NULL,
    description TEXT,
    url_icon    VARCHAR(500),
    criterion   TEXT,
    active      BOOLEAN       NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id)
);

-- 2. Hordes (FK self-referencial nullable)
CREATE TABLE hordes (
    id                 UUID          NOT NULL,
    name               VARCHAR(255)  NOT NULL,
    description        TEXT,
    difficulty         VARCHAR(50)   NOT NULL,
    estimated_duration INTEGER       NOT NULL,
    target_pace        DOUBLE PRECISION,
    horde_id           UUID,
    PRIMARY KEY (id),
    CONSTRAINT fk_horde_parent FOREIGN KEY (horde_id) REFERENCES hordes (id)
);

-- 3. TrainSessions (FK → users, hordes)
CREATE TABLE train_sessions (
    id                  UUID             NOT NULL,
    user_id             UUID             NOT NULL,
    start_date          TIMESTAMP        NOT NULL,
    end_date            TIMESTAMP,
    train_type          VARCHAR(50)      NOT NULL,
    total_distance      DOUBLE PRECISION,
    estimated_calories  DOUBLE PRECISION,
    horde_id            UUID,
    PRIMARY KEY (id),
    CONSTRAINT fk_train_session_user  FOREIGN KEY (user_id)  REFERENCES users  (id),
    CONSTRAINT fk_train_session_horde FOREIGN KEY (horde_id) REFERENCES hordes (id)
);

-- 4. BiometricData (FK → train_sessions)
CREATE TABLE biometric_data (
    id                   UUID             NOT NULL,
    timestamp            TIMESTAMP        NOT NULL,
    bpm                  INTEGER          NOT NULL,
    cadence              DOUBLE PRECISION,
    speed                DOUBLE PRECISION,
    pace                 DOUBLE PRECISION,
    accumulated_distance DOUBLE PRECISION,
    accumulated_calories DOUBLE PRECISION,
    cardiac_zone         VARCHAR(50),
    train_session_id     UUID             NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_biometric_train_session FOREIGN KEY (train_session_id) REFERENCES train_sessions (id)
);

-- 5. UserAchievements (chave composta, FK → users + achievements)
CREATE TABLE user_achievements (
    user_id        UUID NOT NULL,
    achievement_id UUID NOT NULL,
    unlock_date    DATE NOT NULL,
    PRIMARY KEY (user_id, achievement_id),
    CONSTRAINT fk_user_achievement_user        FOREIGN KEY (user_id)        REFERENCES users        (id),
    CONSTRAINT fk_user_achievement_achievement FOREIGN KEY (achievement_id) REFERENCES achievements (id)
);

-- 6. Rankings (FK → users)
CREATE TABLE rankings (
    id           UUID             NOT NULL,
    user_id      UUID             NOT NULL,
    position     INTEGER          NOT NULL,
    score        DOUBLE PRECISION NOT NULL,
    period       VARCHAR(20)      NOT NULL,
    calcule_date DATE             NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ranking_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- 7. Friendships (FK → users x2)
CREATE TABLE friendships (
    id            UUID        NOT NULL,
    requester_id  UUID        NOT NULL,
    recipient_id  UUID        NOT NULL,
    request_date  DATE        NOT NULL,
    response_date DATE,
    status        VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    PRIMARY KEY (id),
    CONSTRAINT fk_friendship_requester FOREIGN KEY (requester_id) REFERENCES users (id),
    CONSTRAINT fk_friendship_recipient FOREIGN KEY (recipient_id) REFERENCES users (id)
);
