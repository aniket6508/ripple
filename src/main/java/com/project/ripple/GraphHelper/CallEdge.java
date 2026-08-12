package com.project.ripple.GraphHelper;

import com.project.ripple.enums.CallType;

public record CallEdge(
        String fromMethodId,
        String toMethodId,
        String rawTarget,
        CallType callType,
        int lineNumber
) {
}
