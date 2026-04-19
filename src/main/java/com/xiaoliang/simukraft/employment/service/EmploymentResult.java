package com.xiaoliang.simukraft.employment.service;

import com.xiaoliang.simukraft.employment.domain.EmploymentAssignment;

public record EmploymentResult(
        boolean success,
        EmploymentErrorCode code,
        String message,
        EmploymentAssignment assignment
) {
    public static EmploymentResult ok(String message, EmploymentAssignment assignment) {
        return new EmploymentResult(true, EmploymentErrorCode.OK, message, assignment);
    }

    public static EmploymentResult error(EmploymentErrorCode code, String message) {
        return new EmploymentResult(false, code, message, null);
    }
}

