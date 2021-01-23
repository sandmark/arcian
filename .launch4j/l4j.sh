#!/bin/sh

set -eux

L4J_CONFIG=$(cygpath -aw .launch4j/release.xml)
launch4jc $L4J_CONFIG
