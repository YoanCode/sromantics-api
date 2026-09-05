package com.sromantics.sromantics_api.enrollment;

import com.sromantics.sromantics_api.entity.Enrollment;
import lombok.Data;

@Data
public class EnrollmentRequest {
    private String studentCourseId;
    private String classId;
    private String startedAt;
    private String endedAt;
    private Enrollment.Status status;
}
