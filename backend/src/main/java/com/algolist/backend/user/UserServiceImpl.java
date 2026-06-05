package com.algolist.backend.user;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	private final UserDao userDao;

	@Override
	public List<UserDto> selectAllUsers() {
		return userDao.selectAllUsers();
	}

	@Override
	public UserDetailDto selectUser(String username) {
		return userDao.selectUser(username);
	}

	@Override
	public boolean insertUser(String username, String password) {
		UserDetailDto user = userDao.selectUser(username);
		
		// 중복된 ID를 사용하고 있는지 확인
		if(user != null) {
			return false;
		}
		
		// 유저 생성 시 1개의 행이 생성되므로 result가 1이여야만 정상 동작으로 간주
		int result = userDao.insertUser(username, password);

		if (result != 1) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public boolean updateUser(String username, String password) {
		int result = userDao.updateUser(username, password);

		if (result != 1) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public boolean deleteUser(String username) {
		int result = userDao.deleteUser(username);

		if (result != 1) {
			return false;
		} else {
			return true;
		}
	}
}
