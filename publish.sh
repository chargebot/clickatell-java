#!/bin/sh
git add .; git commit -m "$2"; git push origin $1; gradle -Prelease.useAutomaticVersion=true release -x checkSnapshotDependencies
