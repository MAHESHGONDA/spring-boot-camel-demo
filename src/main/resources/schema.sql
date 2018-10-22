CREATE TABLE users (
id VARCHAR(255),
program_enrollment_timestamp VARCHAR(255),
program_last_accessed_timestamp VARCHAR(255),
program_accessed_count VARCHAR(255),
used_virtual_care_flag VARCHAR(255),
used_virtual_care_count VARCHAR(255),
used_virtual_care_last_access_timestamp VARCHAR(255),
used_directory_search_flag VARCHAR(255),
used_directory_search_count VARCHAR(255),
used_directory_search_last_access_timestamp VARCHAR(255)
);

CREATE TABLE users_sf (
id VARCHAR(255),
creation_time VARCHAR(255),
last_update_time VARCHAR(255),
program_enrollment_timestamp VARCHAR(255),
program_last_accessed_timestamp VARCHAR(255),
program_accessed_count VARCHAR(255),
used_virtual_care_flag VARCHAR(255),
used_virtual_care_count VARCHAR(255),
used_virtual_care_last_access_timestamp VARCHAR(255),
used_directory_search_flag VARCHAR(255),
used_directory_search_count VARCHAR(255),
used_directory_search_last_access_timestamp VARCHAR(255)
);