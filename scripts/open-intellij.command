#!/bin/zsh
set -euo pipefail

project_dir="${0:A:h:h}"
cd -- "$project_dir"
export PATH="/usr/local/bin:/opt/homebrew/bin:/Applications/Docker.app/Contents/Resources/bin:$PATH"

if ! configured_java=$(/usr/libexec/java_home -v 25 2>/dev/null); then
    print -u2 'Install JDK 25 before opening this Java 25 project.'
    exit 1
fi
export JAVA_HOME="$configured_java"
export PATH="$JAVA_HOME/bin:$PATH"

if [[ ! -f .env ]]; then
    (umask 077; cp .env.example .env)
    print 'Created the local .env file from the lab examples.'
fi

if ! docker info > /dev/null 2>&1; then
    docker desktop start
    ready=false
    for attempt in {1..60}; do
        if docker info > /dev/null 2>&1; then ready=true; break; fi
        sleep 1
    done
    if [[ "$ready" != true ]]; then
        print -u2 'Docker is not ready. Start Docker Desktop and try again.'
        exit 1
    fi
fi

docker compose stop app
docker compose up -d --wait db

project_port=8080
while IFS='=' read -r key value; do
    if [[ "$key" == PORT ]]; then project_port="$value"; fi
done < .env
if lsof -nP -iTCP:"$project_port" -sTCP:LISTEN > /dev/null 2>&1; then
    print -u2 "Port $project_port is in use. Stop the previous Java run before starting another."
    exit 1
fi

print 'Database ready. Select University Management in IntelliJ and click Run.'
if [[ "${1:-}" != --check ]]; then
    /usr/bin/open -a 'IntelliJ IDEA' "$project_dir"
fi
