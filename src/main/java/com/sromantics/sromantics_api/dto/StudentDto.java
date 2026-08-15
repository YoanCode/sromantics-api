package com.sromantics.sromantics_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentDto {
    private String id;
    private String parentId;
    private String name;
    private String gender;
    private String schoolName;
    private String grade;
    private String note;
    private String status;
}
