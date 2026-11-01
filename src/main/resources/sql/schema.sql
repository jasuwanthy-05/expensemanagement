CREATE DATABASE IF NOT EXISTS expense_manager
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE expense_manager;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(190) NOT NULL UNIQUE,
    password_hash VARCHAR(128) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(80) NOT NULL,
    user_id BIGINT NOT NULL,
    color VARCHAR(7) NOT NULL DEFAULT '#12B8A6',
    CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_categories_user_name UNIQUE (user_id, name),
    INDEX idx_categories_user (user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS expenses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    category_id BIGINT NOT NULL,
    expense_date DATE NOT NULL,
    notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_expenses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_expenses_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE,
    INDEX idx_expenses_user_date (user_id, expense_date),
    INDEX idx_expenses_user_category (user_id, category_id),
    INDEX idx_expenses_user_amount (user_id, amount)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS budgets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    monthly_limit DECIMAL(10,2) NOT NULL,
    month TINYINT NOT NULL,
    year SMALLINT NOT NULL,
    CONSTRAINT fk_budgets_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_budgets_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE,
    CONSTRAINT uq_budget_period UNIQUE (user_id, category_id, month, year),
    INDEX idx_budgets_user_period (user_id, year, month)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS recurring_expenses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    category_id BIGINT NOT NULL,
    due_day TINYINT NOT NULL,
    notes VARCHAR(1000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_generated_year SMALLINT,
    last_generated_month TINYINT,
    CONSTRAINT fk_recurring_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_recurring_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE,
    INDEX idx_recurring_user_active (user_id, active)
) ENGINE=InnoDB;
