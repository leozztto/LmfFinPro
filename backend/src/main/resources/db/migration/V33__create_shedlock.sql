-- Trava distribuída dos jobs agendados (ShedLock): com mais de uma réplica, só a que conseguir
-- inserir/atualizar a linha do job executa. Estrutura exigida pelo provider JDBC do ShedLock.
CREATE TABLE shedlock (
    name        VARCHAR(64)  NOT NULL PRIMARY KEY,
    lock_until  TIMESTAMP    NOT NULL,
    locked_at   TIMESTAMP    NOT NULL,
    locked_by   VARCHAR(255) NOT NULL
);
