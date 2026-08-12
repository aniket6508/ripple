package com.project.ripple.parserObjects;

import java.util.List;

public record JavaMethod(
        String id,
        String className,
        String name,
        String returnType,
        List<String> modifiers,
        List<String> parameters,
        int lineNumber,
        List<JavaMethod> calls
) {
}
