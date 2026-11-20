CREATE TABLE IF NOT EXISTS tarefas (
    id         SERIAL PRIMARY KEY,
    titulo     VARCHAR(200) NOT NULL,
    concluida  BOOLEAN      NOT NULL DEFAULT FALSE,
    pod        VARCHAR(100),
    criada_em  TIMESTAMP    NOT NULL DEFAULT NOW()
);
