package com.course.classregistration.domain.enrollment;

import com.course.classregistration.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "enrollments",
        indexes = {
                @Index(name = "idx_enrollment_user_id", columnList = "user_id"),
                @Index(name = "idx_enrollment_course_id", columnList = "course_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Enrollment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnrollmentStatus status;

    // ── 정적 팩토리 메서드 ────────────────────────────────────────────────────
    public static Enrollment create(Long courseId, Long userId) {
        Enrollment enrollment = new Enrollment();
        enrollment.courseId = courseId;
        enrollment.userId = userId;
        enrollment.status = EnrollmentStatus.ENROLLED;
        return enrollment;
    }

    // ── 도메인 비즈니스 메서드 ─────────────────────────────────────────────────
    public boolean isEnrolled() {
        return this.status == EnrollmentStatus.ENROLLED;
    }

    public boolean isCancelled() {
        return this.status == EnrollmentStatus.CANCELLED;
    }

    public void cancel() {
        this.status = EnrollmentStatus.CANCELLED;
    }
}
