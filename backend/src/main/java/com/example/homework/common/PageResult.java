package com.example.homework.common;

import java.util.List;

public record PageResult<T>(long total, long pageNo, long pageSize, List<T> records) {
}
