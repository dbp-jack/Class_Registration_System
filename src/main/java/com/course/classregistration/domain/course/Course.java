package com.course.classregistration.domain.course;

import com.course.classregistration.domain.common.BaseEntity;
import com.course.classregistration.global.exception.BusinessException;
import com.course.classregistration.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "courses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;           // 강좌명

    @Column(nullable = false)
    private String instructor;      // 강사명

    @Column(nullable = false)
    private int maxCapacity;        // 최대 수강 인원

    @Column(nullable = false)
    private int currentEnrollmentCount; // 현재 수강 인원 (카운터 컬럼, 비관적 락으로 보호)

    @Column
    private LocalDateTime enrollmentStartAt;  // 수강 신청 시작일

    @Column
    private LocalDateTime enrollmentEndAt;    // 수강 신청 종료일

    // ── 정적 팩토리 메서드 ────────────────────────────────────────────────────
    public static Course create(String title, String instructor, int maxCapacity,
                                LocalDateTime enrollmentStartAt, LocalDateTime enrollmentEndAt) {
        Course course = new Course();
        course.title = title;
        course.instructor = instructor;
        course.maxCapacity = maxCapacity;
        course.currentEnrollmentCount = 0;
        course.enrollmentStartAt = enrollmentStartAt;
        course.enrollmentEndAt = enrollmentEndAt;
        return course;
    }

    // ── 도메인 비즈니스 메서드 ─────────────────────────────────────────────────

    /**
     * 수강 신청 기간 유효성 검증
     * 기간 정보가 없으면 상시 신청 가능으로 처리
     */
    public void validateEnrollmentPeriod() {
        LocalDateTime now = LocalDateTime.now();
        if (enrollmentStartAt != null && now.isBefore(enrollmentStartAt)) {
            throw new BusinessException(ErrorCode.ENROLLMENT_PERIOD_NOT_STARTED);
        }
        if (enrollmentEndAt != null && now.isAfter(enrollmentEndAt)) {
            throw new BusinessException(ErrorCode.ENROLLMENT_PERIOD_ENDED);
        }
    }

    /**
     * 정원 초과 여부 확인
     */
    public boolean isFull() {
        return currentEnrollmentCount >= maxCapacity;
    }

    /**
     * 수강 인원 증가 (비관적 락 획득 후 호출)
     * 정원 초과 시 예외 발생
     */
    public void increaseEnrollmentCount() {
        if (isFull()) {
            throw new BusinessException(ErrorCode.COURSE_FULL);
        }
        this.currentEnrollmentCount++;
    }

    /**
     * 수강 인원 감소 (수강 취소 시 호출)
     */
    public void decreaseEnrollmentCount() {
        if (this.currentEnrollmentCount > 0) {
            this.currentEnrollmentCount--;
        }
    }

    /**
     * 잔여 자리 수 반환
     */
    public int remainingCapacity() {
        return maxCapacity - currentEnrollmentCount;
    }
}
