package com.example.homework.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.homework.domain.dto.PerfBaselineCreateRequest;
import com.example.homework.domain.entity.PerfBaseline;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.mapper.PerfBaselineMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PerfBaselineService {

    private final PerfBaselineMapper perfBaselineMapper;

    public PerfBaselineService(PerfBaselineMapper perfBaselineMapper) {
        this.perfBaselineMapper = perfBaselineMapper;
    }

    public PerfBaseline create(PerfBaselineCreateRequest request, SysUser actor) {
        PerfBaseline row = new PerfBaseline();
        row.setBaseUrl(request.getBaseUrl().trim());
        row.setPath(request.getPath().trim());
        row.setRequests(request.getRequests());
        row.setSuccess(request.getSuccess());
        row.setFailed(request.getFailed());
        row.setErrorRate(request.getErrorRate());
        row.setMinMs(request.getMinMs());
        row.setAvgMs(request.getAvgMs());
        row.setP95Ms(request.getP95Ms());
        row.setMaxMs(request.getMaxMs());
        row.setGeneratedAt(request.getGeneratedAt() == null ? LocalDateTime.now() : request.getGeneratedAt());
        row.setCreatedBy(actor.getId());
        row.setCreatedAt(LocalDateTime.now());
        perfBaselineMapper.insert(row);
        return row;
    }

    public List<PerfBaseline> list(Integer limit) {
        int safeLimit = limit == null ? 50 : Math.min(Math.max(1, limit), 200);
        return perfBaselineMapper.selectList(new LambdaQueryWrapper<PerfBaseline>()
            .orderByDesc(PerfBaseline::getGeneratedAt)
            .orderByDesc(PerfBaseline::getId)
            .last("LIMIT " + safeLimit));
    }

    public PerfBaseline latest() {
        return perfBaselineMapper.selectOne(new LambdaQueryWrapper<PerfBaseline>()
            .orderByDesc(PerfBaseline::getGeneratedAt)
            .orderByDesc(PerfBaseline::getId)
            .last("LIMIT 1"));
    }
}

