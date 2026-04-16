package com.cumulocity.agent.packaging;

import com.cumulocity.agent.packaging.security.RestEndpointSecurityValidator;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.apache.maven.plugins.annotations.ResolutionScope.RUNTIME;

/**
 * Validates that all REST controller endpoints have proper security annotations.
 *
 * <p>This Mojo scans compiled classes to ensure all REST endpoints are either:
 * <ul>
 *   <li>Annotated with a security annotation (@PreAuthorize, @Secured, @RolesAllowed)</li>
 *   <li>Marked with @UnauthorizedEndpoint to explicitly allow public access</li>
 * </ul>
 *
 * <p>Usage in pom.xml:
 * <pre>
 * &lt;plugin&gt;
 *     &lt;groupId&gt;com.nsn.cumulocity.clients-java&lt;/groupId&gt;
 *     &lt;artifactId&gt;microservice-package-maven-plugin&lt;/artifactId&gt;
 *     &lt;executions&gt;
 *         &lt;execution&gt;
 *             &lt;goals&gt;
 *                 &lt;goal&gt;validate-rest-security&lt;/goal&gt;
 *             &lt;/goals&gt;
 *             &lt;configuration&gt;
 *                 &lt;enabled&gt;true&lt;/enabled&gt;
 *                 &lt;failOnError&gt;true&lt;/failOnError&gt;
 *             &lt;/configuration&gt;
 *         &lt;/execution&gt;
 *     &lt;/executions&gt;
 * &lt;/plugin&gt;
 * </pre>
 */
@Slf4j
@Mojo(name = "validate-rest-security", requiresDependencyResolution = RUNTIME)
public class ValidateRestSecurityMojo extends BaseMicroserviceMojo {

    /**
     * Enable or disable REST endpoint security validation.
     */
    @Parameter(property = "validate-rest-security.enabled", defaultValue = "false")
    private boolean enabled;

    /**
     * Whether to fail the build if security validation errors are found.
     */
    @Parameter(property = "validate-rest-security.failOnError", defaultValue = "true")
    private boolean failOnError;

    /**
     * The directory containing compiled classes (typically target/classes).
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true)
    private File classesDirectory;

    @Override
    public void execute() throws MojoExecutionException {
        if (!enabled) {
            getLog().info("REST endpoint security validation is disabled. " +
                    "Set validate-rest-security.enabled=true to enable it.");
            return;
        }

        getLog().info("Starting REST endpoint security validation...");

        try {
            List<String> classpathElements = project.getRuntimeClasspathElements();
            if (classpathElements == null) {
                classpathElements = new ArrayList<>();
            }
            RestEndpointSecurityValidator validator =
                    new RestEndpointSecurityValidator(classesDirectory, classpathElements);
            List<String> errors = validator.validate();

            if (errors.isEmpty()) {
                getLog().info("✓ All REST endpoints are properly secured!");
                return;
            }

            // Log all errors
            getLog().error("Found " + errors.size() + " unsecured REST endpoints:");
            for (String error : errors) {
                getLog().error("  - " + error);
            }

            if (failOnError) {
                throw new MojoExecutionException(
                        "REST endpoint security validation failed. Found " + errors.size() +
                        " unsecured endpoints. Either add @PreAuthorize/@Secured/@RolesAllowed " +
                        "annotations or mark as @UnauthorizedEndpoint.");
            } else {
                getLog().warn("REST endpoint security validation found issues, but build will continue " +
                        "(failOnError=false)");
            }

        } catch (Exception e) {
            throw new MojoExecutionException("Failed to validate REST endpoint security", e);
        }
    }
}

