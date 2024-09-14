#!/bin/sh

# Compile the Java program if it hasn't been compiled yet
if [ ! -f target/classes/Main.class ]; then
    mkdir -p target/classes
    javac -d target/classes src/main/java/Main.java
fi

# Run the Java program
java -cp target/classes Main "$@"



# #!/bin/sh
# #
# # Use this script to run your program LOCALLY.
# #
# # Note: Changing this script WILL NOT affect how CodeCrafters runs your program.
# #
# # Learn more: https://codecrafters.io/program-interface

# set -e # Exit early if any commands fail

# # Copied from .codecrafters/compile.sh
# #
# # - Edit this to change how your program compiles locally
# # - Edit .codecrafters/compile.sh to change how your program compiles remotely
# (
#   cd "$(dirname "$0")" # Ensure compile steps are run within the repository directory
#   mvn -B package -Ddir=/tmp/codecrafters-build-git-java
# )

# # Copied from .codecrafters/run.sh
# #
# # - Edit this to change how your program runs locally
# # - Edit .codecrafters/run.sh to change how your program runs remotely
# exec java -jar /tmp/codecrafters-build-git-java/java_git.jar "$@"
