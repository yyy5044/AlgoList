CREATE DATABASE IF NOT EXISTS algolist;
USE algolist;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS problems (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    number VARCHAR(50) NOT NULL,
    difficulty VARCHAR(50),
    site VARCHAR(50),
    link VARCHAR(500),
    grade VARCHAR(20),
    solve_count INT DEFAULT 0,
    last_solved_date DATE,
    user_id BIGINT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS problem_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    problem_id BIGINT NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    FOREIGN KEY (problem_id) REFERENCES problems(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS solutions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    algorithm VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL,
    language VARCHAR(50) NOT NULL,
    code TEXT NOT NULL,
    problem_id BIGINT NOT NULL,
    FOREIGN KEY (problem_id) REFERENCES problems(id) ON DELETE CASCADE
);