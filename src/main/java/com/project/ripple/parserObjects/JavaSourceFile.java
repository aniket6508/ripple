package com.project.ripple.parserObjects;

import java.util.List;

public record JavaSourceFile(String path, String packageName, List<String> imports, List<JavaType> types) {
}
