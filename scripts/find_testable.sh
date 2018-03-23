#!/bin/sh

grep -l '^@noaa.coastwatch.test.Testable' `find src -name '*.java'` | sed -e 's/\.java//' | sed -e 's/^src\///' | tr '/' '.'
