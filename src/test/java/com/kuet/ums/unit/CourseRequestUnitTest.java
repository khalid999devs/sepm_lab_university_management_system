package com.kuet.ums.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.kuet.ums.dto.CourseRequest;
import org.junit.jupiter.api.Test;

// Checks that a course request cleans up what the user typed.
class CourseRequestUnitTest {

    @Test
    void courseCodeIsTrimmedAndMadeUpperCase() {
        var request = new CourseRequest("  cse 3230  ", "Web Programming", 2, 30, 5L);

        assertThat(request.code()).isEqualTo("CSE 3230");
    }

    @Test
    void courseTitleIsTrimmed() {
        var request = new CourseRequest("CSE 3230", "  Web Programming  ", 2, 30, 5L);

        assertThat(request.title()).isEqualTo("Web Programming");
    }
}
