package com.maple.extract.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Login {
    String username;
    Object[] emails;
}
