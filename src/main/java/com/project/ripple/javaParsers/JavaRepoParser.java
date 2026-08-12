package com.project.ripple.javaParsers;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.project.ripple.GraphHelper.CallEdge;
import com.project.ripple.GraphHelper.MethodNode;
import com.project.ripple.parserObjects.JavaCall;
import com.project.ripple.parserObjects.JavaMethod;
import com.project.ripple.parserObjects.JavaRepo;
import com.project.ripple.parserObjects.JavaSourceFile;
import com.project.ripple.parserObjects.JavaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@Service
public class JavaRepoParser {
    private static final Set<String> IGNORED_DIRECTORIES = Set.of(
            ".git",
            ".gradle",
            ".idea",
            "build",
            "out",
            "target"
    );

    public JavaRepo parse(Path repoRoot) throws IOException {
        Path normalizedRoot = repoRoot.toAbsolutePath().normalize();

        try (Stream<Path> paths = Files.walk(normalizedRoot)) {
            List<JavaSourceFile> files = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .filter(path -> !isIgnored(normalizedRoot, path))
                    .sorted(Comparator.comparing(Path::toString))
                    .map(path -> parseFile(normalizedRoot, path))
                    .toList();

            return new JavaRepo(normalizedRoot.toString(), files);
        }
    }

    private JavaSourceFile parseFile(Path repoRoot, Path sourceFile) {
        try {
            CompilationUnit compilationUnit = StaticJavaParser.parse(sourceFile);
            String filePath = repoRoot.relativize(sourceFile).toString();
            String packageName = compilationUnit.getPackageDeclaration()
                    .map(packageDeclaration -> packageDeclaration.getName().asString())
                    .orElse("");
            
            // First pass: parse all types and methods to build a method map
            List<JavaType> types = compilationUnit.findAll(TypeDeclaration.class).stream()
                    .map(this::toJavaType)
                    .toList();
            
            // Build a map of methodId -> JavaMethod for resolving calls
            Map<String, JavaMethod> methodMap = buildMethodMap(types, packageName);
            
            // Second pass: resolve method calls
            List<JavaType> resolvedTypes = types.stream()
                    .map(type -> resolveMethodCalls(type, methodMap))
                    .toList();
            
            JavaSourceFile file = new JavaSourceFile(
                    filePath,
                    packageName,
                    compilationUnit.getImports().stream()
                            .map(importDeclaration -> importDeclaration.isStatic()
                                    ? "static " + importDeclaration.getNameAsString()
                                    : importDeclaration.getNameAsString())
                            .toList(),
                    resolvedTypes
            );
            return file;
        } catch (IOException exception) {
            throw new JavaRepoParseException("Failed to read " + sourceFile, exception);
        } catch (RuntimeException exception) {
            throw new JavaRepoParseException("Failed to parse " + sourceFile, exception);
        }
    }

    private Map<String, JavaMethod> buildMethodMap(List<JavaType> types, String packageName) {
        Map<String, JavaMethod> methodMap = new HashMap<>();
        for (JavaType type : types) {
            for (JavaMethod method : type.methods()) {
                String methodId = packageName + "." + type.name() + "#" + method.name();
                methodMap.put(methodId, method);
            }
        }
        return methodMap;
    }

    private JavaType resolveMethodCalls(JavaType type, Map<String, JavaMethod> methodMap) {
        List<JavaMethod> resolvedMethods = type.methods().stream()
                .map(method -> resolveCallsInMethod(method, methodMap))
                .toList();
        return new JavaType(type.name(), type.kind(), type.modifiers(), type.lineNumber(), resolvedMethods);
    }

    private JavaMethod resolveCallsInMethod(JavaMethod method, Map<String, JavaMethod> methodMap) {
        // For now, return as-is since we need MethodCallExpr context to resolve
        // This will be improved when we integrate resolution logic
        return method;
    }

    private JavaType toJavaType(TypeDeclaration<?> typeDeclaration) {
        return new JavaType(
                typeDeclaration.getNameAsString(),
                typeKind(typeDeclaration),
                modifiers(typeDeclaration.getModifiers()),
                lineNumber(typeDeclaration),
                typeDeclaration.getMethods().stream()
                        .map(method -> toJavaMethod(method, null))  // Pass null methodMap initially
                        .toList()
        );
    }

    private MethodNode getMethodNodes(MethodDeclaration methodDeclaration, String filePath) {
        ResolvedMethodDeclaration resolvedMethodDeclaration = methodDeclaration.resolve();
        return new MethodNode(resolvedMethodDeclaration.getQualifiedSignature(), resolvedMethodDeclaration.getClassName(),
                resolvedMethodDeclaration.getName(), filePath, lineNumber(methodDeclaration), true);
    }


    private void edgeTransformationHelper (JavaMethod method, String typeName,String packageName) {
        String params = String.join(", ", method.parameters());
        String callerId = packageName + "." + typeName + "#" + method.name() + "(" + params + ")";
    }

    private JavaMethod toJavaMethod(MethodDeclaration methodDeclaration, Map<String, JavaMethod> methodMap) {
        return new JavaMethod(
                "",  // id - will be filled in by caller
                "",  // className - will be filled in by caller
                methodDeclaration.getNameAsString(),
                methodDeclaration.getTypeAsString(),
                modifiers(methodDeclaration.getModifiers()),
                methodDeclaration.getParameters().stream()
                        .map(parameter -> parameter.getTypeAsString() + " " + parameter.getNameAsString())
                        .toList(),
                lineNumber(methodDeclaration),
                methodMap != null 
                    ? methodDeclaration.findAll(MethodCallExpr.class).stream()
                            .map(callExpr -> resolveMethodCall(callExpr, methodMap))
                            .filter(java.util.Objects::nonNull)
                            .toList()
                    : List.of()  // No resolution possible without methodMap
        );
    }

    private JavaMethod resolveMethodCall(MethodCallExpr methodCallExpr, Map<String, JavaMethod> methodMap) {
        String methodName = methodCallExpr.getNameAsString();
        // Try to resolve using the method map - for now return simple lookup
        // In a full implementation, would use JavaParser's resolver to find qualified name
        for (JavaMethod method : methodMap.values()) {
            if (method.name().equals(methodName)) {
                return method;  // Return first matching method (simplified)
            }
        }
        return null;  // Method not found
    }

    private String typeKind(TypeDeclaration<?> typeDeclaration) {
        if (typeDeclaration instanceof ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
            return classOrInterfaceDeclaration.isInterface() ? "interface" : "class";
        }
        if (typeDeclaration instanceof RecordDeclaration) {
            return "record";
        }
        if (typeDeclaration instanceof EnumDeclaration) {
            return "enum";
        }
        if (typeDeclaration instanceof AnnotationDeclaration) {
            return "@interface";
        }

        return "type";
    }

    private List<String> modifiers(List<Modifier> modifiers) {
        return modifiers.stream()
                .map(modifier -> modifier.getKeyword().asString())
                .toList();
    }

    private int lineNumber(com.github.javaparser.ast.Node node) {
        return node.getBegin()
                .map(position -> position.line)
                .orElse(-1);
    }

    private boolean isIgnored(Path repoRoot, Path path) {
        Path relativePath = repoRoot.relativize(path);

        for (Path part : relativePath) {
            if (IGNORED_DIRECTORIES.contains(part.toString())) {
                return true;
            }
        }

        return false;
    }

    public static class JavaRepoParseException extends RuntimeException {
        public JavaRepoParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
