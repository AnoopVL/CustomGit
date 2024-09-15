# Custom Git Implementation in Java

This is a custom Git implementation from scratch, using Java, that can handle basic Git operations such as initializing a repository, creating commits, and cloning a public repository. This project gives a deep dive into how Git operates at its core, showcasing how fundamental Git objects like blobs, commits, and trees are structured and how Git protocols work behind the scenes.

This custom implementation has been crafted to help developers understand the inner workings of Git beyond its surface-level commands.

## Features

1. **Repository Initialization (`git init`)**:  
   This implementation allows you to initialize a new Git repository, setting up the essential `.git` folder with its associated structure.
2. **Creating Commits (`git commit`)**:  
   Commits are created by manually building Git objects such as trees and blobs. This project shows how each commit is structured and stored in the Git object database.
3. **Cloning Repositories (`git clone`)**:  
   A full clone operation is supported, where the project fetches commit histories and objects from a remote repository. This mimics the behavior of Git’s cloning feature, storing the fetched data in a custom `.git` folder.

## How It Works

### **Git Internals**

- **Blobs, Trees, and Commits**:  
   Git’s data storage revolves around these objects. Blobs store file contents, trees store directory listings, and commits link snapshots of trees over time. In this implementation, I’ve recreated how Git handles these objects, showing how they are structured, hashed, and stored.
- **Transfer Protocols**:  
   Cloning is made possible through the transfer protocols that Git uses to fetch data from a remote repository. This project implements the mechanisms to fetch and store these objects, effectively recreating a mini Git client.

### **.git Directory**

- This implementation mimics the structure of the `.git` directory by manually setting up the directories and files that Git uses to track a repository’s state. This includes the `objects`, `refs`, and `HEAD` files, among others.

### **Command Line Integration**

- The program can be run from the command line using the `your_program.sh` script, where various Git operations can be triggered, such as repository initialization and cloning.

## Getting Started

If you'd like to try out this Git implementation, here are the steps to get started:

1. **Prerequisites**:

   - Ensure you have Java 21 or higher installed on your system.

2. **Running the Program**:

   - Clone this repository and navigate to the project directory.
   - To initialize a new repository, run:

     ```sh
     ./your_program.sh init
     ```

   - To clone a public repository:
     ```sh
     ./your_program.sh clone <repository-url> <target-directory>
     ```

3. **Testing Locally**:
   The program manipulates the `.git` folder in the current directory. To avoid interfering with existing repositories, I recommend testing in a temporary folder:

   ```sh
   mkdir -p /tmp/testing && cd /tmp/testing
   /path/to/your/repo/your_program.sh init
   ```

   You can also create an alias to simplify this process:

   ```sh
   alias mygit=/path/to/your/repo/your_program.sh

   mkdir -p /tmp/testing && cd /tmp/testing
   mygit init
   ```

## Project Structure

- src/main/java/Main.java:
- - This is the main entry point for the custom Git implementation. It contains the logic to handle the Git operations such as init, commit, and clone.

- your_program.sh:
- - This script is used to run the custom Git commands from the command line, serving as an interface to the Java implementation.

## Lessons Learned

Building this Git implementation helped me gain a deeper understanding of Git’s internal workings:

- Understanding of Git Objects:

  Implementing blobs, trees, and commits showed me how Git structures and links files and directories over time.

- Transfer Protocols:

  The cloning feature required me to understand how Git communicates with remote repositories, fetches data, and constructs a local copy of the repository.

## Next Steps

This project is a foundation for a larger Git implementation. Future additions could include:

- Branching and Merging:

  Supporting `git branch`, `git merge`, and related commands to manage different development branches.

- Advanced Protocol Handling:

  Improving the clone process to handle different protocols (HTTP, SSH) and more advanced features of Git’s transport system.

## Acknowledgments

This project was inspired by the [Build Your Own Git](https://app.codecrafters.io/courses/git) Challenge, where I undertook the task of creating a Git implementation. This implementation is customized and built entirely from the ground up.
