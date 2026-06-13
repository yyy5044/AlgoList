package com.algolist.backend.reminder;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReminderDao {
	
	// userId를 기준으로 user_problem 목록 가져오기
	List<ReminderProblemDto> selectReviewCandidates(@Param("userId") Long userId);

	// user_problem_id 값 기반으로 grade 값 UPDATE
	int updateGrade(@Param("userId") Long userId, @Param("userProblemId") Long userProblemId,
			@Param("grade") String grade);

	// UPDATE 이후 갱신된 문제 다시 가져오기
	ReminderProblemDto selectOne(@Param("userId") Long userId, @Param("userProblemId") Long userProblemId);
}
