package com.algolist.backend.user.service;

import com.algolist.backend.user.dto.request.CreateRequestDto;
import com.algolist.backend.user.dto.request.UpdateRequestDto;
import com.algolist.backend.user.dto.response.SolutionActivityResponseDto;
import com.algolist.backend.user.dto.response.UserDetailDto;
import com.algolist.backend.user.dto.response.UserSuspensionInfoDto;

public interface UserService {

	public UserDetailDto selectUser(String username);
	
	public boolean insertUser(CreateRequestDto request);
	
	public boolean updateUser(String username, UpdateRequestDto request);
	
	public boolean deleteUser(String username);

	public UserSuspensionInfoDto selectActiveSuspension(Long userId);

	public SolutionActivityResponseDto selectSolutionActivity(String username);
	
}
