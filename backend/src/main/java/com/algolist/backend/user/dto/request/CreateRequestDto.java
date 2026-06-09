package com.algolist.backend.user.dto.request;

import org.springframework.web.multipart.MultipartFile;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 생성 요청 시 body에 담긴 데이터를 받기 위한 Dto
public class CreateRequestDto {
	private String username;
	private String password;
	private String nickname;
	private MultipartFile profileImage;
}
