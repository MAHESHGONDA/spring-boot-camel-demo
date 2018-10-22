package com.maple.extract.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class User {
    String id;
    String squid;
    String givenName;
    String lastName;
    String primaryEmail;
    Login login;
    Object[] links;
    String createdDate;
    String lastUpdated;
}
