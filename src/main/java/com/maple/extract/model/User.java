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
    String login;
    Object[] links;
    String creationTime;
    String lastUpdated;
}
