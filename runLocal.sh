#!/bin/sh
# Wrapper to ensure Docker is in PATH when running from IDE.
# Docker Desktop on macOS: /usr/local/bin or /Applications/Docker.app/Contents/Resources/bin
export PATH="/usr/local/bin:/opt/homebrew/bin:/Applications/Docker.app/Contents/Resources/bin:$PATH"
exec ./gradlew runLocal "$@"
