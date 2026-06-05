package com.algolist.backend.problem;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProblemServiceImpl implements ProblemService{

	private final ProblemDao dao;
	
	@Override
	public List<UserProblemDto> selectAllByUserId(Long userId) {
		return dao.selectAllByUserId(userId);
	}

	@Override
	public List<ProblemDto> searchProblem(String query) {
		return dao.searchProblem(query);
	}
	
	@Override
	@Transactional
	public UserProblemDto insertUserProblem(Long userId, Long problemId) {
		dao.insertUserProblem(userId, problemId);
		
		return dao.selectOne(userId, problemId);
	}

	@Override
	public int deleteUserProblem(Long userId, Long problemId) {
		return dao.deleteUserProblem(userId, problemId);
	}

	
	



	
}
