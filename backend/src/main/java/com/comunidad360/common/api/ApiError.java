package com.comunidad360.common.api;

import java.net.URI;
import java.util.Map;

public record ApiError(
        URI type,
        String title,
        int status,
        String code,
        String messageKey,
        Map<String, Object> params,
        String detail,
        String traceId
) {
}
