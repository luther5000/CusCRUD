\set ON_ERROR_STOP on

-- Conectar explicitamente ao banco da aplicação antes de criar extensões e objetos
\connect cuscrud

-- Habilitar a extensão necessaria no banco da aplicação
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Garantir que tabelas, indices e constraints pertençam ao usuário da aplicação
SET ROLE cuscrud_app;

-- 1. Tabela de usuários
CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    login VARCHAR(255) UNIQUE NOT NULL,
    passwd TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_users_login ON users (login);

-- 2. Tabela de inventários
CREATE TABLE inventories (
    inv_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    inv_name VARCHAR(255) NOT NULL
);

-- 3. Tabela de Acesso
CREATE TABLE inventory_access (
    user_id UUID REFERENCES users(user_id) ON DELETE CASCADE,
    inv_id UUID REFERENCES inventories(inv_id) ON DELETE CASCADE,
    role INT4 NOT NULL,
    PRIMARY KEY (user_id, inv_id)
);
CREATE INDEX idx_inventory_access_user ON inventory_access (user_id);
CREATE INDEX idx_inventory_access_inv ON inventory_access (inv_id);

-- 4. Tabela de Tipos
CREATE TABLE types (
    type_id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    imagem BYTEA,
    inv_id UUID NOT NULL REFERENCES inventories(inv_id) ON DELETE CASCADE,
    UNIQUE (inv_id, nome),
    UNIQUE (inv_id, type_id)
);
CREATE INDEX idx_types_inv ON types (inv_id);

-- 5. Tabela de Produtos
CREATE TABLE products (
    product_id BIGSERIAL PRIMARY KEY,
    type_id INT8 NOT NULL,
    marca VARCHAR(255) NULL,
    dataValidade TIMESTAMP WITH TIME ZONE NULL,
    unidade INT8 NULL CHECK (unidade IS NULL OR unidade BETWEEN 0 AND 999999999999999999),
    unidadeMedida VARCHAR(255) NULL,
    quantidade INT8 NOT NULL DEFAULT 0 CHECK (quantidade BETWEEN 0 AND 999999999999999999),
    inv_id UUID NOT NULL REFERENCES inventories(inv_id) ON DELETE CASCADE,
    FOREIGN KEY (inv_id, type_id) REFERENCES types(inv_id, type_id) ON DELETE RESTRICT
);
CREATE INDEX idx_products_inv ON products (inv_id);
CREATE INDEX idx_products_type_id ON products (type_id);
CREATE INDEX idx_products_inv_type_id ON products (inv_id, type_id);

RESET ROLE;
