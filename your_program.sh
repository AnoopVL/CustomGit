#!/bin/bash

# Ensure we're in the right directory
cd "$(dirname "$0")"

# Compile the Java code
javac src/main/java/Main.java

# Run the Java program with the provided arguments
java -cp src/main/java Main "$@"



# #!/bin/bash

# if [ "$1" == "clone" ]; then
#     repo_url=$2
#     target_dir=$3
#     if [ -z "$repo_url" ] || [ -z "$target_dir" ]; then
#         echo "Usage: ./your_program.sh clone <repo_url> <target_directory>"
#         exit 1
#     fi

#     # Create repository structure
#     mkdir -p "$target_dir/.git/objects"
#     mkdir -p "$target_dir/.git/refs/heads"
#     echo "ref: refs/heads/main" > "$target_dir/.git/HEAD"

#     # Fetch refs to get the latest commit hash
#     git_url="${repo_url%/}.git"
#     refs=$(git ls-remote --heads "$git_url")
#     commit_hash=$(echo "$refs" | grep "refs/heads/main" | cut -f1)

#     if [ -z "$commit_hash" ]; then
#         echo "Failed to fetch the latest commit hash"
#         exit 1
#     fi

#     echo "Latest commit hash: $commit_hash"

#     # Fetch the commit object
#     object_dir="$target_dir/.git/objects/${commit_hash:0:2}"
#     object_file="$object_dir/${commit_hash:2}"
#     mkdir -p "$object_dir"

#     if [ ! -f "$object_file" ]; then
#         echo "Fetching commit object $commit_hash"
#         curl -sfL -o "$object_file" "$git_url/objects/${commit_hash:0:2}/${commit_hash:2}"
#     fi

#     if [ -f "$object_file" ]; then
#         echo "Commit object $commit_hash fetched successfully."
#     else
#         echo "Failed to fetch commit object $commit_hash"
#         exit 1
#     fi

#     echo "Repository cloned to: $target_dir"
# fi