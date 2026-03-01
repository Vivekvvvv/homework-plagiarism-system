package com.example.homework.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course")
public class Course {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String courseCode;
    private String courseName;
    private Long teacherId;
    private String semester;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
