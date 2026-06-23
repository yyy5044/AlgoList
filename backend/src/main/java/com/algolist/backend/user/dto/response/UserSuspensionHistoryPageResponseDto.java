package com.algolist.backend.user.dto.response;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
//페이징을 위해 정지 이력 목록과 페이지, 사이즈를 가지고 있는 Dto
public class UserSuspensionHistoryPageResponseDto {
	private List<UserSuspensionHistoryDto> suspensions;
	private int page;
	private int size;
	private long totalCount;
	private int totalPages;
}
