package com.algolist.backend.user;

import java.util.List;

public interface UserService {
	
	public List<UserDto> selectAllUsers();

	public UserPageResponseDto selectUsers(int page, int size, String status, String searchType, String keyword);
	
	public UserDetailDto selectUser(String username);
	
	public boolean insertUser(CreateRequestDto request);
	
	public boolean updateUser(String username, UpdateRequestDto request);
	
	public boolean deleteUser(String username);

	public boolean suspendUser(String username, SuspendUserRequestDto request, Long adminId);

	public boolean releaseUserSuspension(String username, ReleaseSuspensionRequestDto request, Long adminId);

	public UserSuspensionInfoDto selectActiveSuspension(Long userId);
	
}
