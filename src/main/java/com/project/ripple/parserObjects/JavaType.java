package com.project.ripple.parserObjects;

import java.util.List;

public record JavaType(String name, String kind, List<String> modifiers, int lineNumber, List<JavaMethod> methods) {
}
