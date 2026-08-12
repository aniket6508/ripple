package com.project.ripple.parserObjects;

import java.util.List;

public record JavaRepo(String root, List<JavaSourceFile> files) {
}
