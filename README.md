# Introduction

This project built a Git version control system in Java from scratch. 

# Prerequisite
- `IntelliJ IDEA Community Edition 2022.1.3  `
- `JDK-17.0.3.7-hotspot`

# Setup
Download `hamcrest-core-1.3.jar` and `junit-4.13.2.jar` in `library` and add them to IntelliJ project library. 
> **Details:** In IntelliJ, `File` -> `Project Structure` -> `Project Settings` -> `Libraries` -> `New Project Library` -> Add and finish.

Then you can use it like the real git with the commands below.

# Supported Commands 

### init
`java gitlite.Main init`  
Creates a new Gitlite version-control system in the current directory.
### add
`java gitlite.Main add [file name]`  
Adds a copy of the file as it currently exists to the staging area.
### commit
`java gitlite.Main commit [message]`  
Saves a snapshot of tracked files in the current commit and staging area so they can be restored at a later time, creating a new commit. 
### rm
`java gitlite.Main rm [file name]`  
Unstage the file if it is currently staged for addition. If the file is tracked, remove it from the working directory if the user has not already done so.
### log
`java gitlite.Main log`  
Starting at the current head commit, display information about each commit backwards along the commit tree until the initial commit.
### global-log
`java gitlite.Main global-log`  
Like log, except displays information about all commits ever made.
### find
`java gitlite.Main find [commit message]`  
Prints out the ids of all commits that have the given commit message.
### status
`java gitlite.Main status`  
Example
```
=== Branches ===
*master
other-branch
  
=== Staged Files ===
a.txt
a2.txt
  
=== Removed Files ===
b2.txt
  
=== Modifications Not Staged For Commit ===
c1.txt (deleted)
c2.txt (modified)
  
=== Untracked Files ===
random.stuff
```
### checkout
`java gitlite.Main checkout -- [file name]`  
Takes the version of the file as it exists in the head commit and puts it in the working directory, overwriting the version of the file that’s already there if there is one. The new version of the file is not staged.   
`java gitlite.Main checkout [commit id] -- [file name]`  
Like the former command, except takes from the given commit.  
`java gitlite.Main checkout [branch name]`  
Takes all files in the commit at the head of the given branch, and puts them in the working directory, overwriting the versions of the files that are already there if they exist. Also, at the end of this command, the given branch will now be considered the current branch (HEAD). Any files that are tracked in the current branch but are not present in the checked-out branch are deleted.
### branch
`java gitlite.Main branch [branch name]`  
Creates a new branch with the given name, and points it at the current head commit.
### rm-branch
`java gitlite.Main rm-branch [branch name]`  
Deletes the branch with the given name. 
### reset
`java gitlite.Main reset [commit id]`  
Checks out all the files tracked by the given commit. Removes tracked files that are not present in that commit. Also moves the current branch’s head to that commit node. 
### merge
`java gitlite.Main merge [branch name]`  
Merges files from the given branch into the current branch. For each file, there are several possible cases, consider if it exists in current branch, merge branch, and the split point. 

# Reference
[Gitlite](https://sp21.datastructur.es/materials/proj/proj2/proj2)
