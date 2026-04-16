package com.cumulocity.agent.packaging.security;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * Validates that all REST controller endpoints have proper security annotations.
 *
 * This validator scans compiled classes to find all REST controllers and their endpoints,
 * then verifies that each endpoint is either:
 * 1. Marked with a security annotation (@PreAuthorize, @Secured, @RolesAllowed)
 * 2. Marked with @UnauthorizedEndpoint annotation to exempt it from security checks
 */
@Slf4j
public class RestEndpointSecurityValidator {

    private static final String[] SECURITY_ANNOTATIONS = {
            "org.springframework.security.access.prepost.PreAuthorize",
            "org.springframework.security.access.annotation.Secured",
            "jakarta.annotation.security.RolesAllowed",
            "javax.annotation.security.RolesAllowed"
    };

    private static final String UNAUTHORIZED_ENDPOINT_ANNOTATION =
            "com.cumulocity.microservice.security.annotation.UnauthorizedEndpoint";

    private static final String[] REST_CONTROLLER_ANNOTATIONS = {
            "org.springframework.web.bind.annotation.RestController",
            "org.springframework.web.bind.annotation.Controller"
    };

    private static final String[] ENDPOINT_ANNOTATIONS = {
            "org.springframework.web.bind.annotation.GetMapping",
            "org.springframework.web.bind.annotation.PostMapping",
            "org.springframework.web.bind.annotation.PutMapping",
            "org.springframework.web.bind.annotation.DeleteMapping",
            "org.springframework.web.bind.annotation.PatchMapping",
            "org.springframework.web.bind.annotation.RequestMapping"
    };

    private final File outputDirectory;
    private final ClassLoader classLoader;

    public RestEndpointSecurityValidator(File outputDirectory, List<String> classpathElements) throws IOException {
        this.outputDirectory = outputDirectory;
        this.classLoader = createClassLoader(outputDirectory, classpathElements);
    }

    /**
     * Validates all REST controller endpoints in the output directory.
     *
     * @return List of validation errors (empty if all validations pass)
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        try {
            Set<String> controllerClasses = findRestControllers();
            log.info("Found {} REST controller classes", controllerClasses.size());
            for (String controllerClass : controllerClasses) {
                log.info("Found REST controller class: {}", controllerClass);
            }

            for (String className : controllerClasses) {
                try {
                    Class<?> controllerClass = classLoader.loadClass(className);
                    errors.addAll(validateController(controllerClass));
                } catch (Throwable e) {
                    log.debug("Failed to load and validate class {}: {}", className, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error during REST endpoint security validation", e);
            errors.add("Failed to validate REST endpoints: " + e.getMessage());
        }

        return errors;
    }

    /**
     * Finds all REST controller classes in the output directory.
     */
    private Set<String> findRestControllers() throws IOException {
        Set<String> classes = new HashSet<>();
        Set<String> controllers = new HashSet<>();
        findClassesRecursive(outputDirectory, classes);
        for (String className : classes) {
            try {
                Class<?> controllerClass = classLoader.loadClass(className);
                if (isRestController(controllerClass)) {
                    controllers.add(className);
                }
            }
            catch (Throwable e) {
                // Catch all errors including ClassFormatError, ClassNotFoundException, etc.
                log.debug("Failed to load and validate class {}: {}", className, e.getMessage());
            }
        }
        return controllers;
    }

    /**
     * Recursively finds all class files in the directory tree.
     */
    private void findClassesRecursive(File dir, Set<String> classNames) {
        if (!dir.isDirectory()) {
            return;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                findClassesRecursive(file, classNames);
            } else if (file.getName().endsWith(".class")) {
                String className = getClassNameFromFile(file);
                if (className != null) {
                    classNames.add(className);
                }
            }
        }
    }

    /**
     * Converts a file path to a class name.
     */
    private String getClassNameFromFile(File file) {
        String relativePath = file.getAbsolutePath()
                .substring(outputDirectory.getAbsolutePath().length() + 1);
        relativePath = relativePath.replace(File.separator, ".");
        if (relativePath.endsWith(".class")) {
            relativePath = relativePath.substring(0, relativePath.length() - 6);
        }
        // Skip inner classes and lambda classes for simplicity
        if (relativePath.contains("$")) {
            return null;
        }
        return relativePath;
    }

    /**
     * Validates a single REST controller class.
     */
    private List<String> validateController(Class<?> controllerClass) {
        List<String> errors = new ArrayList<>();

        if (!isRestController(controllerClass)) {
            return errors;
        }

        log.debug("Validating REST controller: {}", controllerClass.getName());

        Method[] methods = controllerClass.getDeclaredMethods();
        for (Method method : methods) {
            if (isEndpointMethod(method)) {
                List<String> methodErrors = validateEndpointMethod(controllerClass, method);
                errors.addAll(methodErrors);
            }
        }

        return errors;
    }

    /**
     * Checks if a class is a REST controller.
     */
    private boolean isRestController(Class<?> clazz) {
        for (String annotationName : REST_CONTROLLER_ANNOTATIONS) {
            if (hasAnnotation(clazz, annotationName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a method is a REST endpoint (has request mapping annotation).
     */
    private boolean isEndpointMethod(Method method) {
        for (String annotationName : ENDPOINT_ANNOTATIONS) {
            if (hasAnnotation(method, annotationName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validates a single endpoint method.
     */
    private List<String> validateEndpointMethod(Class<?> controllerClass, Method method) {
        List<String> errors = new ArrayList<>();

        // Check if endpoint is exempt from security checks
        if (hasAnnotation(method, UNAUTHORIZED_ENDPOINT_ANNOTATION)) {
            log.debug("Endpoint {} is marked as @UnauthorizedEndpoint, skipping validation",
                    formatMethodName(controllerClass, method));
            return errors;
        }

        // Check if endpoint has security annotation
        if (!hasSecurityAnnotation(method)) {
            String error = String.format(
                    "REST endpoint not secured: %s. Must be annotated with @PreAuthorize, @Secured, " +
                    "@RolesAllowed, or @UnauthorizedEndpoint.",
                    formatMethodName(controllerClass, method));
            errors.add(error);
        }

        return errors;
    }

    /**
     * Checks if a method has any security annotation.
     */
    private boolean hasSecurityAnnotation(Method method) {
        for (String annotationName : SECURITY_ANNOTATIONS) {
            if (hasAnnotation(method, annotationName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a class has a specific annotation by name.
     */
    private boolean hasAnnotation(Class<?> clazz, String annotationName) {
        try {
            Class<?> annotationClass = classLoader.loadClass(annotationName);
            return clazz.getAnnotation((Class<? extends java.lang.annotation.Annotation>) annotationClass) != null;
        } catch (ClassNotFoundException | ClassCastException e) {
            return false;
        }
    }

    /**
     * Checks if a method has a specific annotation by name.
     */
    private boolean hasAnnotation(Method method, String annotationName) {
        try {
            Class<?> annotationClass = classLoader.loadClass(annotationName);
            return method.getAnnotation((Class<? extends java.lang.annotation.Annotation>) annotationClass) != null;
        } catch (ClassNotFoundException | ClassCastException e) {
            return false;
        }
    }

    /**
     * Formats a method name for display.
     */
    private String formatMethodName(Class<?> clazz, Method method) {
        return clazz.getName() + "." + method.getName();
    }

    /**
     * Creates a URLClassLoader covering the output directory and all runtime classpath entries.
     */
    private ClassLoader createClassLoader(File outputDirectory, List<String> classpathElements) throws IOException {
        List<URL> urls = new ArrayList<>();
        urls.add(outputDirectory.toURI().toURL());
        for (String element : classpathElements) {
            urls.add(new File(element).toURI().toURL());
        }
        return new URLClassLoader(urls.toArray(new URL[0]), Thread.currentThread().getContextClassLoader());
    }
}

