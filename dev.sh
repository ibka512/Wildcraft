#!/bin/sh
set -eu

task_java_home="${WILDCRAFT_JAVA_HOME:-}"
if [ -z "$task_java_home" ] && [ -x /usr/libexec/java_home ]; then
    task_java_home="$(/usr/libexec/java_home -v 25 2>/dev/null || true)"
fi
if [ -z "$task_java_home" ]; then
    for task_candidate in /opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home /usr/local/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home; do
        if [ -x "$task_candidate/bin/javac" ]; then
            task_java_home="$task_candidate"
            break
        fi
    done
fi
if [ ! -x "$task_java_home/bin/javac" ]; then
    echo '需要 JDK 25。安装后设置 WILDCRAFT_JAVA_HOME，或使用 Homebrew 的 openjdk@25。' >&2
    exit 1
fi
case "$("$task_java_home/bin/javac" -version 2>&1)" in
    'javac 25'*) ;;
    *) echo 'Wildcraft 当前需要 JDK 25，请检查 WILDCRAFT_JAVA_HOME。' >&2; exit 1 ;;
esac

export JAVA_HOME="$task_java_home"
export PATH="$JAVA_HOME/bin:$PATH"
task_root="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
cd "$task_root"
if [ "$(uname -s)" = Darwin ]; then
    # IntelliJ scans these directories directly and can mistake AppleDouble
    # metadata for another wrapper configuration or a run configuration.
    for task_metadata in "$task_root/gradle/wrapper/._gradle-wrapper.properties" "$task_root"/.idea/runConfigurations/._*.xml; do
        if [ -f "$task_metadata" ] && [ "$(od -An -N4 -tx1 "$task_metadata" | tr -d ' \n')" = 00051607 ]; then
            rm -f "$task_metadata"
        fi
    done
    # Keep Gradle's mutable caches on APFS even when the source lives on ExFAT.
    export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/Library/Caches/Wildcraft/gradle}"
    task_project_id="$(printf '%s' "$task_root" | cksum | awk '{print $1}')"
    task_project_cache="$HOME/Library/Caches/Wildcraft/projects/$task_project_id"
    exec ./gradlew --project-cache-dir "$task_project_cache" "$@"
fi
exec ./gradlew "$@"
