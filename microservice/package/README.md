### Usage
To use the plugin you need to add it to the pom.xml

```
    <plugins>
        <plugin>
            <groupId>com.nsn.cumulocity.clients-java</groupId>
            <artifactId>microservice-package-maven-plugin</artifactId>
            <version>latest</version>
        </plugin>
    </plugins>
```

### Packaging

To build microservice package from command line:
```
mvn clean install microservice:package 
```

or adding to pom.xml

```
      <plugins>
            <plugin>
                <groupId>com.nsn.cumulocity.clients-java</groupId>
                <artifactId>microservice-package-maven-plugin</artifactId>
                <version>latest</version>

                <executions>
                    <execution>
                        <goals>
                            <goal>package</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
```

and run
```
mvn clean install
```


### Uploading
To upload microservice first configure settings.xml
```
	<server>
	    <id>microservice</id>
	    <username>management/admin</username>
	    <password>****</password>
	    <configuration>
		    <url>http://cumulocity.default.svc.cluster.local</url>
	    </configuration>
	</server>
```

Then run
```
mvn clean install microservice:package microservice:upload
```

Or if is already built
```
mvn microservice:upload
```

# Build-time REST Endpoint Security Validation

This feature validates that all REST controller endpoints have proper security annotations at build time.

## Overview

The `validate-rest-security` Maven goal scans compiled classes to ensure all REST endpoints are either:
1. **Secured** - annotated with `@PreAuthorize`, `@Secured`, or `@RolesAllowed`
2. **Explicitly unsecured** - annotated with `@UnauthorizedEndpoint`

If unsecured endpoints are found and not exempted, the build fails with clear error messages.

## Usage

### 1. Add to Your Microservice pom.xml

```xml
<plugin>
    <groupId>com.nsn.cumulocity.clients-java</groupId>
    <artifactId>microservice-package-maven-plugin</artifactId>
    <version>${project.version}</version>
    <executions>
        <execution>
            <id>validate-rest-security</id>
            <goals>
                <goal>validate-rest-security</goal>
            </goals>
            <phase>prepare-package</phase>
            <configuration>
                <enabled>true</enabled>
                <failOnError>true</failOnError>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 2. Annotate Your Endpoints

**Secure endpoint (requires authentication/authorization):**
```java
@RestController
@RequestMapping("/api")
public class UserController {
    
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public List<User> getUsers() {
        return userService.findAll();
    }
}
```

**Unsecured endpoint (public access):**
```java
@RestController
@RequestMapping("/api")
public class HealthController {
    
    @UnauthorizedEndpoint("Public health check endpoint")
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
```

## Configuration Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `enabled` | `false` | Enable/disable the validation (must be explicitly enabled) |
| `failOnError` | `true` | Fail build if unsecured endpoints found (set to false to warn only) |

## Supported Security Annotations

The validator recognizes these security annotations:
- `@org.springframework.security.access.prepost.PreAuthorize`
- `@org.springframework.security.access.annotation.Secured`
- `@jakarta.annotation.security.RolesAllowed`
- `@javax.annotation.security.RolesAllowed`

## Example Output

**Success:**
```
[INFO] Starting REST endpoint security validation...
[INFO] Found 5 REST controller classes
[INFO] ✓ All REST endpoints are properly secured!
```

**Failure:**
```
[ERROR] Found 2 unsecured REST endpoints:
[ERROR]   - REST endpoint not secured: com.example.UserController.deleteAll. 
            Must be annotated with @PreAuthorize, @Secured, @RolesAllowed, or @UnauthorizedEndpoint.
[ERROR]   - REST endpoint not secured: com.example.ConfigController.reset. 
            Must be annotated with @PreAuthorize, @Secured, @RolesAllowed, or @UnauthorizedEndpoint.
```

## Imports Required

Use the `@UnauthorizedEndpoint` annotation from:
```java
import com.cumulocity.microservice.security.annotation.UnauthorizedEndpoint;
```

This annotation is provided by `microservice-security` module.

