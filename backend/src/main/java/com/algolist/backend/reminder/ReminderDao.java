package com.algolist.backend.reminder;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReminderDao {
	
	// userId를 기준으로 user_problem 목록 가져오기
	List<ReminderProblemDto> selectReviewCandidates(@Param("userId") Long userId);
}
