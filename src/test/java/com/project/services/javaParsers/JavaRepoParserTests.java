package com.project.services.javaParsers;

import com.project.ripple.javaParsers.JavaRepoParser;
import com.project.ripple.parserObjects.JavaCall;
import com.project.ripple.parserObjects.JavaMethod;
import com.project.ripple.parserObjects.JavaRepo;
import com.project.ripple.parserObjects.JavaSourceFile;
import com.project.ripple.parserObjects.JavaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class JavaRepoParserTests {
    private final JavaRepoParser parser = new JavaRepoParser();

    @TempDir
    Path repoRoot;

    @Test
    void parsesJavaFilesInRepository() throws IOException {
        Path sourceFile = repoRoot.resolve("src/main/java/com/example/ProductService.java");
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, """
                package com.example;

                import com.example.products.Product;
                import com.example.products.ProductMapper;
                import com.example.products.ProductRepository;

                public class ProductService {
                    public Product getProduct(String id) {
                        ProductRepository.findById(id);
                        return buildProduct();
                    }

                    private Product buildProduct() {
                        return ProductMapper.toProduct();
                    }
                }
                """);

        JavaRepo repo = parser.parse(repoRoot);

        assertThat(repo.files()).hasSize(1);
        JavaSourceFile parsedFile = repo.files().getFirst();
        assertThat(parsedFile.path()).isEqualTo("src/main/java/com/example/ProductService.java");
        assertThat(parsedFile.packageName()).isEqualTo("com.example");
        assertThat(parsedFile.imports())
                .containsExactly(
                        "com.example.products.Product",
                        "com.example.products.ProductMapper",
                        "com.example.products.ProductRepository"
                );

        JavaType parsedType = parsedFile.types().getFirst();
        assertThat(parsedType.name()).isEqualTo("ProductService");
        assertThat(parsedType.kind()).isEqualTo("class");
        assertThat(parsedType.modifiers()).containsExactly("public");
        assertThat(parsedType.methods())
                .extracting(JavaMethod::name)
                .containsExactly("getProduct", "buildProduct");

        JavaMethod getProduct = parsedType.methods().getFirst();
        assertThat(getProduct.modifiers()).containsExactly("public");
        assertThat(getProduct.returnType()).isEqualTo("Product");
        assertThat(getProduct.parameters()).containsExactly("String id");
        assertThat(getProduct.calls())
                .extracting(JavaCall::target)
                .containsExactly("ProductRepository.findById", "buildProduct");

        JavaMethod buildProduct = parsedType.methods().get(1);
        assertThat(buildProduct.modifiers()).containsExactly("private");
        assertThat(buildProduct.calls())
                .extracting(JavaCall::target)
                .containsExactly("ProductMapper.toProduct");
    }

    @Test
    void ignoresGeneratedAndBuildDirectories() throws IOException {
        Path sourceFile = repoRoot.resolve("src/main/java/com/example/Visible.java");
        Path targetFile = repoRoot.resolve("target/generated-sources/Hidden.java");
        Files.createDirectories(sourceFile.getParent());
        Files.createDirectories(targetFile.getParent());
        Files.writeString(sourceFile, "package com.example; public class Visible {}");
        Files.writeString(targetFile, "package com.example; public class Hidden {}");

        JavaRepo repo = parser.parse(repoRoot);

        assertThat(repo.files())
                .extracting(JavaSourceFile::path)
                .containsExactly("src/main/java/com/example/Visible.java");
    }
}
