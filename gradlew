#!/usr/bin/env sh

#
# Copyright 2015 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

# Attempt to set APP_HOME
# Resolve links: $0 may be a link
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
    ls -ld "$PRG"
    link=`expr "$PRG" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD="maximum"

warn () {
    echo "$*"
}

die () {
    echo
    echo "$*"
    echo
    exit 1
}

# OS specific support (must be 'true' or 'false').
cygwin=false
msys=false
darwin=false
nonstop=false
case "`uname`" in
  CYGWIN* )
    cygwin=true
    ;;
  Darwin* )
    darwin=true
    ;;
  MINGW* )
    msys=true
    ;;
  NONSTOP* )
    nonstop=true
    ;;
esac

# For Cygwin or MSYS, switch paths to Windows format before running java
if [ "$cygwin" = "true" -o "$msys" = "true" ] ; then
    APP_HOME=`cygpath --path --mixed "$APP_HOME"`
    APP_BASE_NAME=`cygpath --path --mixed "$APP_BASE_NAME"`
    CLASSPATH=`cygpath --path --mixed "$CLASSPATH"`

    JAVACMD=`cygpath --unix "$JAVACMD"`

    # We build the pattern for arguments to be converted via cygpath
    ROOTDIRSRAW=`find -L / -maxdepth 3 -type d -name gradle 2>/dev/null | grep -E '/gradle$'`
    [ -z "$ROOTDIRSRAW" ] && ROOTDIRSRAW=`find -L / -maxdepth 3 -type d -name gradle 2>/dev/null`
    for dir in $ROOTDIRSRAW ; do
        ROOTDIR=`echo "$dir" | sed 's|//||'`
        if [ -f "$ROOTDIR/bin/gradle.jar" ]; then
            GRADLE_HOME="$ROOTDIR"
            break
        fi
    done
    if [ -z "$GRADLE_HOME" ]; then
        GRADLE_HOME=`dirname "$JAVACMD"`
        GRADLE_HOME=`expr "$GRADLE_HOME" : '\(.*\)/bin/.*$' \| "$GRADLE_HOME" : '\(.*\)/bin$'`
    fi
    # Fallback
    GRADLE_HOME=${GRADLE_HOME:-.}
fi

# For Mingw, ensure paths are in UNIX format before anything is touched
if [ "$msys" = "true" ]; then
    START "`pwd`"
    cd `dirname "$PRG"`
    CMD_DIR=`pwd`
fi

if [ ! -x "$JAVACMD" ] ; then
    die "JAVA_HOME is not set and no 'java' command could be found in your PATH."
fi

if [ -z "$JAVA_HOME" ] ; then
    die "The JAVA_HOME environment variable is not set and no 'java' command could be found in your PATH."
fi

# Increase the maximum file descriptors if we can.
if [ "$cygwin" = "false" -a "$darwin" = "false" -a "$nonstop" = "false" ] ; then
    MAX_FD_LIMIT=`ulimit -H -n`
    if [ $? -eq 0 ] ; then
        if [ "$MAX_FD_LIMIT" != "unlimited" ] ; then
            MAX_FD=`expr $MAX_FD_LIMIT`
            if [ $MAX_FD -lt 1024 ] ; then
                warn "Could not set maximum file descriptor limit to $MAX_FD on this system."
            else
                if [ "$verbose" = "true" ] ; then
                    echo "Setting maxfile descriptor limit to $MAX_FD"
                fi
                ulimit -n $MAX_FD
            fi
        else
            if [ "$verbose" = "true" ] ; then
                echo "Could not query system maximum file descriptor limit."
            fi
        fi
    else
        warn "Could not query$JAVA_HOME/bin/java -version" >&2
        exit 1
    fi
fi

# For Darwin, add options to specify how the application appears in the dock
if $darwin; then
    GRADLE_OPTS="$GRADLE_OPTS \"-Xdock:name=$APP_NAME\" \"-Xdock:icon=$APP_HOME/media/gradle.icns\""
fi

# For Cygwin or MSYS, switch paths to Windows format before running java
if [ "$cygwin" = "true" -o "$msys" = "true" ] ; then
    APP_HOME=`cygpath --path --mixed "$APP_HOME"`
    CLASSPATH=`cygpath --path --mixed "$CLASSPATH"`

    # We build the pattern for arguments to be converted via cygpath
    ROOTDIRSRAW=`find -L / -maxdepth 3 -type d -name gradle 2>/dev/null | grep -E '/gradle$'`
    [ -z "$ROOTDIRSRAW" ] && ROOTDIRSRAW=`find -L / -maxdepth 3 -type d -name gradle 2>/dev/null`
fi

exec "$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
