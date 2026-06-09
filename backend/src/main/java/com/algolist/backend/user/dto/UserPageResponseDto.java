package com.algolist.backend.user.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 페이징을 위해 User 목록과 페이지, 사이즈를 가지고 있는 Dto
public class UserPageResponseDto {
	private List<UserDto> users;
	private int page;
	private int size;
	private long totalCount;
	private int totalPages;
}
