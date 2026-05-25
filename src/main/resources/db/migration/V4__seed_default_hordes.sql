-- V4: Hordas padrao disponiveis para selecao no app
-- IDs fixos deixam o seed idempotente e facilitam testes/integracoes.

INSERT INTO hordes (
    id,
    name,
    description,
    difficulty,
    estimated_duration,
    target_pace,
    is_adaptive,
    horde_id
) VALUES
    (
        '11111111-1111-1111-1111-111111111111',
        'Horda Inicial',
        'Desafio leve para corredores iniciantes.',
        'EASY',
        20,
        8.0,
        FALSE,
        NULL
    ),
    (
        '22222222-2222-2222-2222-222222222222',
        'Horda Selvagem',
        'Desafio intermediario para manter ritmo constante.',
        'MEDIUM',
        30,
        6.5,
        FALSE,
        NULL
    ),
    (
        '33333333-3333-3333-3333-333333333333',
        'Horda Implacavel',
        'Desafio intenso para corredores avancados.',
        'HARD',
        45,
        5.0,
        FALSE,
        NULL
    )
ON CONFLICT (id) DO NOTHING;
