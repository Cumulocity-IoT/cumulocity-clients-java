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

### Docker image build options

The `package` goal builds the microservice image and writes it to the microservice zip. The `push` goal pushes the image to `agent-package.container.registry`.

| Parameter in pom.xml | Command line property | Default | Description |
|---|---|---|---|
| `dockerBuildArchs` | `microservice.package.dockerBuildArchs` | `buildSpec.targetBuildArchitectures` of `cumulocity.json`, otherwise `amd64` | Comma-separated target platforms, for example `amd64,arm64`. The former name `targetBuildArchs` still works in pom.xml, but it is deprecated. |
| `useBuildx` | `microservice.package.buildx` | `false` | Builds the image with `docker buildx`. Required for multi-platform images. |
| `dockerBuildNoCache` | `microservice.package.dockerBuildNoCache` | `true` | Builds every layer again and pulls the base image, so the image gets the current packages, for example Java. Set to `false` to use the docker build cache, for example for offline builds. |
| `failOnJavaVersionMismatch` | `microservice.package.failOnJavaVersionMismatch` | `false` | Fails the build when the platforms of a multi-platform image have different Java versions. Otherwise the build logs a warning. |
| `dockerBuildNetwork` | `microservice.package.dockerBuildNetwork` | docker default | Network for `RUN` instructions. Multi-platform builds support only `default`, `host`, and `none`. |
| `dockerBuildTimeout` | `microservice.package.dockerBuildTimeout` | `360` | Timeout in seconds of each docker command, for example the image build. |
| `deleteImage` | `microservice.package.deleteImage` | `true` | Removes the image from the local docker daemon after the zip is written (docker daemon build). |
| `dockerCli` | `microservice.package.dockerCli` | `docker` | Docker CLI binary used for `docker buildx`. |

#### Target platforms

Cumulocity supports `amd64` (`linux/amd64`) and `arm64` (`linux/arm64`). The plugin also accepts the aliases `x86_64`, `aarch64`, and `arm64v8`. Other platforms are passed to docker as given, and the build logs a warning that Cumulocity may not support them.

Set the platforms in pom.xml, on the command line, or in `cumulocity.json`:

```json
"buildSpec": {
    "targetBuildArchitectures": ["amd64", "arm64"]
}
```

The pom.xml and command line value overrides `cumulocity.json`.

#### Docker daemon build (default)

The plugin builds the image through the docker daemon API, one platform at a time:

* `package` writes one zip per platform. With one platform, the zip is `<artifactId>-<version>.zip`. With several platforms, the zip for `linux/amd64` is `<artifactId>-<version>.zip`, and the zips of the other platforms add the platform to the name, for example `<artifactId>-<version>-linux-arm64.zip`.
* `push` pushes the image built last as `<name>:<version>` and `<name>:latest`. With several platforms, the build logs a warning, because all platforms share the same tags.

#### Multi-platform images with docker buildx

With `-Dmicroservice.package.buildx=true` the plugin builds the image with `docker buildx`:

* `package` always writes one zip, `<artifactId>-<version>.zip`. For one platform, the zip contains the image in `docker save` format, like the docker daemon build. For several platforms, it contains one multi-platform image (OCI image index).
* `push` pushes the image of the last `package` run, so the registry gets exactly the image of the zip. For several platforms, it pushes one image index for all platforms under `<name>:<version>` and `<name>:latest`. If `package` did not build an image for the same platforms, `push` builds it first.
* With `skip.microservice.package=true`, `package` loads the image into the docker daemon instead of writing a zip. A multi-platform image is loaded only if the docker daemon uses the containerd image store.

```
mvn clean install -Dmicroservice.package.buildx=true -Dmicroservice.package.dockerBuildArchs=amd64,arm64
```

Prerequisites:

* Docker with the buildx plugin 0.10 or newer (Docker Engine 23 or newer with `docker-buildx-plugin`, or Docker Desktop).
* To build `arm64` on an `amd64` host, QEMU emulation must be installed. The emulation is lost after a reboot of the host:
  ```
  docker run --privileged --rm tonistiigi/binfmt --install arm64
  ```

The plugin does not change your buildx builders. It keeps its buildx state in `target/buildx` and the built image in `target/buildx-image`. The plugin creates a temporary `docker-container` builder and removes it after the build, so the build cache of one build is not available to the next one. If the docker daemon uses the containerd image store, the plugin uses the built-in `default` builder and its build cache instead.

The plugin runs these checks:

| Check | Result | How to fix |
|---|---|---|
| docker buildx 0.10 or newer is installed | The build fails. | Install the buildx plugin. |
| The network is `default`, `host`, or `none` for several platforms | The build fails. | Change `dockerBuildNetwork`. |
| The builder lists all supported target platforms | The build logs a warning. The build may fail later with `exec format error`. | Install QEMU emulation, see above. |
| All platforms have the same Java version (several platforms, after the build) | The build logs a warning. With `failOnJavaVersionMismatch=true`, the build fails. | See the next section. |

#### Same Java version on all platforms

All platforms of a multi-platform image must run the same Java version. The Java version can differ between platforms when:

* the build cache holds an older Java layer for one platform,
* the Alpine package mirrors of the architectures are not in sync for a short time, or
* a custom base image has different Java versions for its platforms.

The plugin builds a multi-platform image once and runs `java -version` on every platform of the result. Then it exports or pushes exactly the layers it checked. If the versions differ, the build logs a warning with the version of each platform. To fail the build instead, set `-Dmicroservice.package.failOnJavaVersionMismatch=true`. After a mismatch, build again later or use a base image with a fixed Java version.

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

