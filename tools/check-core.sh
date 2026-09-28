#!/usr/bin/env bash
set -euo pipefail
check_dir=$(mktemp -d)
trap 'rm -rf "$check_dir"' EXIT
javac -d "$check_dir" \
    src/client/java/dev/cameron/nightwatch/engine/Action.java \
    src/client/java/dev/cameron/nightwatch/engine/Director.java \
    src/client/java/dev/cameron/nightwatch/engine/Personality.java \
    src/client/java/dev/cameron/nightwatch/engine/Provider.java \
    src/client/java/dev/cameron/nightwatch/engine/RuleWriter.java \
    src/client/java/dev/cameron/nightwatch/engine/Scene.java \
    src/client/java/dev/cameron/nightwatch/loom/*.java \
    src/test/java/dev/cameron/nightwatch/engine/DirectorCheck.java \
    src/test/java/dev/cameron/nightwatch/loom/LoomSequenceCheck.java
java -cp "$check_dir" dev.cameron.nightwatch.engine.DirectorCheck
java -cp "$check_dir" dev.cameron.nightwatch.loom.LoomSequenceCheck
