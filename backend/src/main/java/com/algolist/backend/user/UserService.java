package com.algolist.backend.user;

import java.util.List;

public interface UserService {
	
	public List<UserDto> selectAllUsers();
	
	public UserDetailDto selectUser(String username);
	
	public boolean insertUser(CreateRequestDto request);
	
	public boolean updateUser(String username, UpdateRequestDto request);
	
	public boolean deleteUser(String username);
	
}
