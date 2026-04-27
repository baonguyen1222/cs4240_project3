#!/bin/bash

# 1. Create a bin directory for compiled classes
mkdir -p bin

# 2. Compile all files into the bin directory
# Using the -d flag tells javac where to put the .class files
javac -d bin $(find materials/src -name "*.java")

if [ $? -eq 0 ]; then
    echo "Build Successful: Classes are in the 'bin' folder."
else
    echo "Build Failed."
    exit 1
fi