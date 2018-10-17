package com.example.demo.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserResponse {
    List<User> content;
    String nextURI;
}
