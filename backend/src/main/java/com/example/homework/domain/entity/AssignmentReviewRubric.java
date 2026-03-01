package com.example.homework.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("assignment_review_rubric")
public class AssignmentReviewRubric {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long assignmentId;
    private String rubricJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
