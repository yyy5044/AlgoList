package com.algolist.backend;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.Map;

@Mapper
public interface UserMapper {
    Map<String, Object> findByUsername(@Param("username") String username);
}