package com.example.homework.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

import java.util.List;

/**
 * Utility class for safe query parameter normalization and common query patterns.
 */
public final class QueryHelper {

    private QueryHelper() {
    }

    /**
     * Clamp a nullable limit into [1, maxLimit] with a default fallback.
     */
    public static int safeLimit(Integer limit, int defaultLimit, int maxLimit) {
        if (limit == null) {
            return defaultLimit;
        }
        return Math.min(Math.max(1, limit), maxLimit);
    }

    /**
     * Execute a query with descending order on the given column.
     * Encapsulates the common pattern: query.orderByDesc(column) + mapper.selectList(query).
     */
    public static <T> List<T> descList(BaseMapper<T> mapper,
                                       LambdaQueryWrapper<T> query,
                                       SFunction<T, ?> orderColumn) {
        query.orderByDesc(orderColumn);
        return mapper.selectList(query);
    }
}
