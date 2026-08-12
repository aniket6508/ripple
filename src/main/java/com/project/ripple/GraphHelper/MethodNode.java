package com.project.ripple.GraphHelper;

public record MethodNode(
        String id,
        String className,
        String methodName,
        String filePath,
        int lineNumber,
        boolean projectSource
) {
}
