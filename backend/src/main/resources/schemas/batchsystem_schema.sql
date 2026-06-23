CREATE DATABASE IF NOT EXISTS batch_system;

USE batch_system;

# 배치 실패 페이지 기록: 수집 배치에서 실패한 페이지를 기록하고 재시도 대상으로 관리
CREATE TABLE IF NOT EXISTS failed_pages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source VARCHAR(50) NOT NULL,
    page_offset INT NOT NULL,
    page_size INT NOT NULL,
    reason TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS failed_problems (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    site VARCHAR(50),              -- BOJ, PROGRAMMERS 등
    number VARCHAR(50),            -- 문제 번호
    file_url VARCHAR(1000),        -- README를 다시 가져올 GitHub URL
    reason TEXT,          -- 실패한 이미지 URL 등
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);