package com.algolist.backend.user.service;

import java.util.List;

import com.algolist.backend.user.dto.CreateRequestDto;
import com.algolist.backend.user.dto.ReleaseSuspensionRequestDto;
import com.algolist.backend.user.dto.SuspendUserRequestDto;
import com.algolist.backend.user.dto.UpdateRoleRequestDto;
import com.algolist.backend.user.dto.UpdateRequestDto;
import com.algolist.backend.user.dto.UserDetailDto;
import com.algolist.backend.user.dto.UserDto;
import com.algolist.backend.user.dto.UserPageResponseDto;
import com.algolist.backend.user.dto.UserSuspensionInfoDto;

public interface UserService {
	
	public List<UserDto> selectAllUsers();

	public UserPageResponseDto selectUsers(int page, int size, String status, String searchType, String keyword);
	
	public UserDetailDto selectUser(String username);
	
	public boolean insertUser(CreateRequestDto request);
	
	public boolean updateUser(String username, UpdateRequestDto request);
	
	public boolean deleteUser(String username);

	public boolean suspendUser(String username, SuspendUserRequestDto request, Long adminId);

	public boolean releaseUserSuspension(String username, ReleaseSuspensionRequestDto request, Long adminId);

	public boolean updateUserRole(String username, UpdateRoleRequestDto request, Long adminId);

	public UserSuspensionInfoDto selectActiveSuspension(Long userId);
	
}
