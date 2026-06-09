package com.algolist.backend.user.service;

import java.util.List;

import com.algolist.backend.user.dto.UserDto;
import com.algolist.backend.user.dto.request.ReleaseSuspensionRequestDto;
import com.algolist.backend.user.dto.request.SuspendUserRequestDto;
import com.algolist.backend.user.dto.request.UpdateRoleRequestDto;
import com.algolist.backend.user.dto.response.UserDetailDto;
import com.algolist.backend.user.dto.response.UserPageResponseDto;

public interface AdminUserService {

	public List<UserDto> selectAllUsers();

	public UserPageResponseDto selectUsers(int page, int size, String status, String searchType, String keyword);

	public UserDetailDto selectUser(String username);

	public boolean deleteUser(String username);

	public boolean suspendUser(String username, SuspendUserRequestDto request, Long adminId);

	public boolean releaseUserSuspension(String username, ReleaseSuspensionRequestDto request, Long adminId);

	public boolean updateUserRole(String username, UpdateRoleRequestDto request, Long adminId);
}
