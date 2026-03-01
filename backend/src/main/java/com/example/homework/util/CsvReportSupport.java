package com.example.homework.util;

import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;

public final class CsvReportSupport {

    private CsvReportSupport() {
    }

    public static void appendMeta(StringBuilder sb, String reportType, Map<String, String> meta) {
        sb.append("metaField,metaValue").append("\n");
        sb.append("reportType,").append(reportType == null ? "" : reportType).append("\n");
        sb.append("generatedAt,").append(LocalDateTime.now()).append("\n");
        if (meta != null) {
            for (Map.Entry<String, String> entry : meta.entrySet()) {
                sb.append(entry.getKey()).append(",").append(entry.getValue()).append("\n");
            }
        }
        sb.append("\n");
    }

    public static String csvEscape(String input) {
        if (!StringUtils.hasText(input)) {
            return "";
        }
        String escaped = input.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}

