#!/bin/sh

# Compile the Java program if it hasn't been compiled yet
if [ ! -f target/classes/Main.class ]; then
    mkdir -p target/classes
    javac -d target/classes src/main/java/Main.java
fi

# Run the Java program
java -cp target/classes Main "$@"

