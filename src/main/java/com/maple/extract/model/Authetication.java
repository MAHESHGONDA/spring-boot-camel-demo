package com.maple.extract.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Authetication {
    private String access_token;
    private String token_type;
    private String expires_in;
    private String scope;
}
