package com.sromantics.sromantics_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParentDto {
    private String id;
    private String name;
    private String phone;
    private String email;
    private String relationship;
}
