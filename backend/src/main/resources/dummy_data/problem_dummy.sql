INSERT INTO problems (title, number, difficulty, site, link, grade, solve_count, last_solved_date, user_id) VALUES
('A+B',          '1000', 'Bronze5', 'BOJ', 'https://www.acmicpc.net/problem/1000', 'Bronze3',  5, '2026-05-20', 1),
('별 찍기 - 1',   '2438', 'Bronze4', 'BOJ', 'https://www.acmicpc.net/problem/2438', 'Silver5',  3, '2026-05-21', 1),
('피보나치 함수', '1003', 'Silver3', 'BOJ', 'https://www.acmicpc.net/problem/1003', 'Silver4', 1, '2026-05-22', 1),
('DFS와 BFS',    '1260', 'Silver2', 'BOJ', 'https://www.acmicpc.net/problem/1260', 'Silver2',0, NULL,         1),
('두 용액',      '2470', 'Gold5',   'BOJ', 'https://www.acmicpc.net/problem/2470', 'Gold4', 2, '2026-05-19', 1);

INSERT INTO problem_categories (problem_id, category_name)
          SELECT problem_id, '수학'              FROM problems WHERE number='1000' AND user_id=1
UNION ALL SELECT problem_id, '구현'              FROM problems WHERE number='1000' AND user_id=1
UNION ALL SELECT problem_id, '구현'              FROM problems WHERE number='2438' AND user_id=1
UNION ALL SELECT problem_id, '다이나믹 프로그래밍' FROM problems WHERE number='1003' AND user_id=1
UNION ALL SELECT problem_id, '그래프'            FROM problems WHERE number='1260' AND user_id=1
UNION ALL SELECT problem_id, 'BFS'              FROM problems WHERE number='1260' AND user_id=1
UNION ALL SELECT problem_id, 'DFS'              FROM problems WHERE number='1260' AND user_id=1
UNION ALL SELECT problem_id, '이분 탐색'         FROM problems WHERE number='2470' AND user_id=1;

-- 조회 (selectAll 매퍼와 동일한 LEFT JOIN 형태)
SELECT p.problem_id, p.title, p.number, p.user_id, c.category_name
FROM problems p
LEFT JOIN problem_categories c ON p.problem_id = c.problem_id
WHERE p.user_id = 1;

-- 삭제 검증 (매퍼와 동일: user_id + problem_id 둘 다 매칭)
DELETE FROM problems WHERE user_id=1 AND problem_id=<지울ID>;