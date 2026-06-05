-- 문제 카탈로그
INSERT INTO problems (title, number, difficulty, site, link) VALUES
('A+B',          '1000', 'Bronze5', 'BOJ', 'https://www.acmicpc.net/problem/1000'),
('별 찍기 - 1',   '2438', 'Bronze4', 'BOJ', 'https://www.acmicpc.net/problem/2438'),
('피보나치 함수', '1003', 'Silver3', 'BOJ', 'https://www.acmicpc.net/problem/1003'),
('DFS와 BFS',    '1260', 'Silver2', 'BOJ', 'https://www.acmicpc.net/problem/1260'),
('두 용액',      '2470', 'Gold5',   'BOJ', 'https://www.acmicpc.net/problem/2470');

-- 문제 카테고리
INSERT INTO problem_categories (problem_id, category_name)
SELECT problem_id, '수학' FROM problems WHERE number='1000'
UNION ALL SELECT problem_id, '구현' FROM problems WHERE number='1000'
UNION ALL SELECT problem_id, '사칙연산' FROM problems WHERE number='1000'
UNION ALL SELECT problem_id, '구현' FROM problems WHERE number='2438'
UNION ALL SELECT problem_id, '다이나믹 프로그래밍' FROM problems WHERE number='1003'
UNION ALL SELECT problem_id, '그래프 이론' FROM problems WHERE number='1260'
UNION ALL SELECT problem_id, 'BFS' FROM problems WHERE number='1260'
UNION ALL SELECT problem_id, 'DFS' FROM problems WHERE number='1260'
UNION ALL SELECT problem_id, '이분 탐색' FROM problems WHERE number='2470'
UNION ALL SELECT problem_id, '두 포인터' FROM problems WHERE number='2470'
UNION ALL SELECT problem_id, '정렬' FROM problems WHERE number='2470';
