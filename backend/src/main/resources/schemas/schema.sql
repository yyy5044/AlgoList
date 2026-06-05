CREATE DATABASE IF NOT EXISTS algolist;
USE algolist;

# 유저 테이블
CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    username VARCHAR(50) NOT NULL UNIQUE,
    nickname VARCHAR(50),
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',

    profile_image_url VARCHAR(500),
    bio VARCHAR(500),

    account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL
);

# 정지 이력 기록 테이블
CREATE TABLE IF NOT EXISTS user_suspensions (
    suspension_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT NOT NULL,
    reason VARCHAR(255),
    suspended_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    suspended_until DATETIME NOT NULL,
    suspended_by BIGINT NULL,

    released_at DATETIME NULL,
    released_by BIGINT NULL,
    release_reason VARCHAR(255) NULL,

    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (suspended_by) REFERENCES users(user_id),
    FOREIGN KEY (released_by) REFERENCES users(user_id)
);


# 문제 테이블
CREATE TABLE IF NOT EXISTS problems (
    problem_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    number VARCHAR(50) NOT NULL,
    difficulty VARCHAR(50),
    site VARCHAR(50),
    link VARCHAR(500),
    grade VARCHAR(20),
    solve_count INT DEFAULT 0,
    last_solved_date DATE,
    user_id BIGINT,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS problem_categories (
    problem_category_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    problem_id BIGINT NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    FOREIGN KEY (problem_id) REFERENCES problems(problem_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS solutions (
    solution_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    algorithm VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL,
    language VARCHAR(50) NOT NULL,
    code TEXT NOT NULL,
    problem_id BIGINT NOT NULL,
    FOREIGN KEY (problem_id) REFERENCES problems(problem_id) ON DELETE CASCADE
);