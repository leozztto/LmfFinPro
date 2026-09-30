-- Regra global (user_id nulo) do motor de categorização automática: mesma ideia de "categoria
-- padrão do sistema" que já existia em categories.user_id, agora também para category_rules —
-- sugestão pronta desde o primeiro extrato importado, sem o usuário precisar cadastrar nada. Uma
-- correção manual do usuário sempre cria/reforça uma regra própria (ver ImportApplicationService),
-- que passa a ter prioridade sobre a global para o mesmo padrão.
ALTER TABLE category_rules ALTER COLUMN user_id DROP NOT NULL;

-- Categorias padrão do sistema (user_id nulo) usadas como alvo das regras globais abaixo.
INSERT INTO categories (user_id, name, type, color, icon) VALUES
    (NULL, 'Transporte',        'EXPENSE', '#EAB308', NULL),
    (NULL, 'Alimentação',       'EXPENSE', '#EF4444', NULL),
    (NULL, 'Mercado',           'EXPENSE', '#22C55E', NULL),
    (NULL, 'Saúde',             'EXPENSE', '#14B8A6', NULL),
    (NULL, 'Educação',          'EXPENSE', '#6366F1', NULL),
    (NULL, 'Lazer',             'EXPENSE', '#EC4899', NULL),
    (NULL, 'Assinaturas',       'EXPENSE', '#A855F7', NULL),
    (NULL, 'Moradia',           'EXPENSE', '#F97316', NULL),
    (NULL, 'Compras',           'EXPENSE', '#F59E0B', NULL),
    (NULL, 'Salário',           'INCOME',  '#2AD6A5', NULL);

-- Padrão de descritivo -> nome da categoria global acima (match por substring, case-insensitive,
-- ver CategoryRule.matches). Peso inicial 1, igual a uma regra recém-criada pelo usuário.
INSERT INTO category_rules (user_id, pattern, category_id, weight)
SELECT NULL, seed.pattern, c.id, 1
FROM (VALUES
    ('UBER',            'Transporte'),
    ('99APP',           'Transporte'),
    ('99POP',           'Transporte'),
    ('CABIFY',          'Transporte'),
    ('POSTO IPIRANGA',  'Transporte'),
    ('POSTO SHELL',     'Transporte'),
    ('BILHETE UNICO',   'Transporte'),
    ('IFOOD',           'Alimentação'),
    ('RAPPI',           'Alimentação'),
    ('MCDONALDS',       'Alimentação'),
    ('BURGER KING',     'Alimentação'),
    ('CARREFOUR',       'Mercado'),
    ('PAO DE ACUCAR',   'Mercado'),
    ('ASSAI',           'Mercado'),
    ('ATACADAO',        'Mercado'),
    ('DROGARIA',        'Saúde'),
    ('FARMACIA',        'Saúde'),
    ('DROGASIL',        'Saúde'),
    ('UNIMED',          'Saúde'),
    ('HAPVIDA',         'Saúde'),
    ('AMIL',            'Saúde'),
    ('UDEMY',           'Educação'),
    ('ALURA',           'Educação'),
    ('COURSERA',        'Educação'),
    ('NETFLIX',         'Lazer'),
    ('SPOTIFY',         'Lazer'),
    ('DISNEY PLUS',     'Lazer'),
    ('HBO MAX',         'Lazer'),
    ('PRIME VIDEO',     'Lazer'),
    ('STEAM',           'Lazer'),
    ('ICLOUD',          'Assinaturas'),
    ('GOOGLE ONE',      'Assinaturas'),
    ('YOUTUBE PREMIUM', 'Assinaturas'),
    ('OFFICE 365',      'Assinaturas'),
    ('ADOBE',           'Assinaturas'),
    ('ENERGISA',        'Moradia'),
    ('CPFL ENERGIA',    'Moradia'),
    ('SABESP',          'Moradia'),
    ('COMGAS',          'Moradia'),
    ('CEMIG',           'Moradia'),
    ('VIVO FIBRA',      'Moradia'),
    ('CLARO NET',       'Moradia'),
    ('AMAZON',          'Compras'),
    ('MERCADO LIVRE',   'Compras'),
    ('SHOPEE',          'Compras'),
    ('MAGAZINE LUIZA',  'Compras'),
    ('AMERICANAS',      'Compras'),
    ('SALARIO',         'Salário'),
    ('FOLHA DE PAGAMENTO', 'Salário')
) AS seed(pattern, category_name)
JOIN categories c ON c.user_id IS NULL AND c.name = seed.category_name;
