package com.project.ripple.controllers;

import com.project.ripple.javaParsers.JavaRepoParser;
import com.project.ripple.parserObjects.JavaRepo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Path;

@RestController
@Tag(name = "Java repository parser", description = "Parses Java repositories and returns files, types, methods, and method calls.")
public class RippleWebController {
    private final JavaRepoParser javaRepoParser;

    public RippleWebController(JavaRepoParser javaRepoParser) {
        this.javaRepoParser = javaRepoParser;
    }

    @GetMapping("/ParseRepo")
    @Operation(
            summary = "Parse a Java repository",
            description = "Walks the supplied local repository path and returns Java source metadata plus method call relationships.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Repository parsed successfully",
                            content = @Content(schema = @Schema(implementation = JavaRepo.class))
                    ),
                    @ApiResponse(responseCode = "500", description = "Repository could not be read or parsed")
            }
    )
    public JavaRepo parseFile(
            @Parameter(description = "Absolute or relative path to the Java repository to parse", example = "/Users/aniket/Downloads/ripple")
            @RequestParam String repoPath
    ) throws IOException {
        return javaRepoParser.parse(Path.of(repoPath));
    }
}
