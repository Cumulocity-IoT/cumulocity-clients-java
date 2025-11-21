FROM @package.docker.baseImage@

RUN apk update
# Install coreutils if not present. Command exit 0 is required if coreutils are presend int the image.
# If the coreutils are present ! command -v coreutils >/dev/null 2>&1 will set the exit code of the command to 1 failing the docker build 
RUN command -v apk >/dev/null 2>&1 && ! command -v coreutils >/dev/null 2>&1 && apk add --no-cache coreutils; exit 0
# Install java if not present.
RUN command -v apk >/dev/null 2>&1 && ! command -v java >/dev/null 2>&1 && apk add --no-cache openjdk@package.java-version@; exit 0
# Validate that required dependencies are installed
RUN command -v coreutils >/dev/null 2>&1
RUN command -v java >/dev/null 2>&1
COPY etc/ /etc/@package.directory@/
ADD resources/* /data/
RUN  mkdir /otel;
WORKDIR /otel
RUN  include_otel_agent=$(echo "@package.otel-java-agent-include@" | tr '[:upper:]' '[:lower:]') \
     && if [ $include_otel_agent = "true" ]; \
        then \
          wget @package.otel-java-agent-download-url@; \
        fi
RUN chmod +x /data/entrypoint.sh
ENTRYPOINT /data/entrypoint.sh
