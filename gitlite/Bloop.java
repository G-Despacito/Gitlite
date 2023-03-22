package gitlite;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static gitlite.Utils.*;

/** Implemented Git commands.
 *
 *  @author G-Despacito
 */

public class Bloop {
    /** Magic number. **/
    private static final int SHA1LEN = 40;

    /** Creates a new Gitlite version-control system in the current directory.
     * This system will automatically start with one commit: a commit that contains
     * no files and has the commit message initial commit (just like that, with no
     * punctuation). It will have a single branch: main, which initially points to
     * this initial commit, and main will be the current branch. The timestamp for
     * this initial commit will be 00:00:00 UTC, Thursday, 1 January 1970 in
     * whatever format you choose for dates (this is called “The (Unix) Epoch”,
     * represented internally by the time 0.) Since the initial commit in all
     * repositories created by Gitlite will have exactly the same content, it
     * follows that all repositories will automatically share this commit (they
     * will all have the same UID) and all commits in all repositories will trace
     * back to it. **/
    public static void init(String[] args) {
        validateNumArgs("init", args, 1);

        // Failure cases: If the file does not exist, print the error message
        // File does not exist. and exit without changing anything.
        if (Repository.GITLET_DIR.exists()) {
            exitWithError("A Gitlite version-control system already exists in "
                    + "the current directory.");
        }

        // Creates a new Gitlite version-control system in the current directory.
        Repository repo = new Repository();

        Commit initial = new Commit("initial commit");
        initial.saveCommit();

        Repository.updateBranch("main", initial.getSha1());
        Repository.writeCommitEditMsg(initial.getMessage());

        return;

    }

    /** Adds a copy of the file as it currently exists to the staging area (see the
     * description of the commit command). For this reason, adding a file is also
     * called staging the file for addition. Staging an already-staged file overwrites
     * the previous entry in the staging area with the new contents. The staging area
     * should be somewhere in .gitlite. If the current working version of the file is
     * identical to the version in the current commit, do not stage it to be added,
     * and remove it from the staging area if it is already there (as can happen when
     * a file is changed, added, and then changed back to it’s original version). The
     * file will no longer be staged for removal (see gitlite rm), if it was at the
     * time of the command. **/
    public static void add(String[] args) {
        validateNumArgs("add", args, 2);

        // Failure cases: If the file does not exist, print the error message File
        // does not exist. and exit without changing anything.
        File file = join(Repository.CWD, args[1]);
        if (!file.exists() && !file.isFile()) {
            exitWithError("File does not exist.");
        }

        if (join(Repository.STAGE_RM_DIR, args[1]).exists()) {
            writeContents(join(Repository.CWD, args[1]),
                    readContents(join(Repository.STAGE_RM_DIR, args[1])));
            join(Repository.STAGE_RM_DIR, args[1]).delete();
            return;
        }

        File copy = join(Repository.STAGE_ADD_DIR, args[1]);
        if (Commit.fromCommitFile(Repository.getFetchHead()).getBlobs().
                get(sha1(file.getName(), readContents(file))) != null
                && Commit.fromCommitFile(Repository.getFetchHead()).getBlobs().
                get(sha1(file.getName(), readContents(file))).equals(file.getName())) {
            if (copy.exists()) {
                copy.delete();
            }
            return;
        }

        writeContents(copy, readContents(file));
        if (!join(Repository.BLOBS_DIR, sha1(file.getName(), readContents(file))).exists()) {
            Commit.saveBlob(file);
        }
        return;
    }


    /** Saves a snapshot of tracked files in the current commit and staging area so
     *  they can be restored at a later time, creating a new commit. The commit is
     *  said to be tracking the saved files. By default, each commit’s snapshot of
     *  files will be exactly the same as its parent commit’s snapshot of files; it
     *  will keep versions of files exactly as they are, and not update them. A
     *  commit will only update the contents of files it is tracking that have been
     *  staged for addition at the time of commit, in which case the commit will now
     *  include the version of the file that was staged instead of the version it got
     *  from its parent. A commit will save and start tracking any files that were
     *  staged for addition but weren’t tracked by its parent. Finally, files tracked
     *  in the current commit may be untracked in the new commit as a result being
     *  staged for removal by the rm command (below).
     *
     *  The bottom line: By default a commit has the same file contents as its parent.
     *  Files staged for addition and removal are the updates to the commit. Of
     *  course, the date (and likely the mesage) will also different from the parent.**/
    public static void commit(String[] args) {

        // Failure cases: If no files have been staged, abort. Print the
        // message No changes added to the commit. Every commit must have
        // a non-blank message. If it doesn’t, print the error message
        // Please enter a commit message. It is not a failure for tracked
        // files to be missing from the working directory or changed in
        // the working directory. Just ignore everything outside the
        // .gitlite directory entirely.
        if (args.length > 2) {
            exitWithError("Incorrect operands.");
        }
        if (args.length < 2 || args[1].equals("")) {
            exitWithError("Please enter a commit message.");
        }
        if (plainFilenamesIn(Repository.STAGE_ADD_DIR).size() == 0
                && plainFilenamesIn(Repository.STAGE_RM_DIR).size() == 0) {
            exitWithError("No changes added to the commit.");
        }


        // Set default.
        Commit headCommit = Commit.fromCommitFile(Repository.getFetchHead());
        Commit newCommit = new Commit(args[1], headCommit.getSha1());
        newCommit.setBlobs(headCommit.getBlobs());

        // Update add.
        for (String fileName : plainFilenamesIn(Repository.STAGE_ADD_DIR)) {
            if (newCommit.getBlobs().containsValue(fileName)) {
                newCommit.removeBlob(fileName);
            }
            newCommit.addBlob(join(Repository.STAGE_ADD_DIR, fileName));
        }

        clearDir(Repository.STAGE_ADD_DIR);

        // Update remove.
        for (String fileName : plainFilenamesIn(Repository.STAGE_RM_DIR)) {
            if (newCommit.getBlobs().containsValue(fileName)) {
                newCommit.removeBlob(fileName);
            }
        }

        clearDir(Repository.STAGE_RM_DIR);

        newCommit.saveCommit();

        // Update Head, Main or other branch, CommitEditMsg.
        Repository.updateBranch(Repository.getHead(), newCommit.getSha1());
        Repository.writeCommitEditMsg(newCommit.getMessage());

        return;

    }

    /** Commit with a second parent. **/
    public static void commitWithSecondParent(String[] args) {

        // Failure cases: If no files have been staged, abort. Print the
        // message No changes added to the commit. Every commit must have
        // a non-blank message. If it doesn’t, print the error message
        // Please enter a commit message. It is not a failure for tracked
        // files to be missing from the working directory or changed in
        // the working directory. Just ignore everything outside the
        // .gitlite directory entirely.
        if (args.length > 3) {
            exitWithError("Incorrect operands.");
        }
        if (args.length < 3 || args[1].equals("")) {
            exitWithError("Please enter a commit message.");
        }
        if (plainFilenamesIn(Repository.STAGE_ADD_DIR).size() == 0
                && plainFilenamesIn(Repository.STAGE_RM_DIR).size() == 0) {
            exitWithError("No changes added to the commit.");
        }


        // Set default.
        Commit headCommit = Commit.fromCommitFile(Repository.getFetchHead());
        Commit newCommit = new Commit(args[1], headCommit.getSha1());
        newCommit.setBlobs(headCommit.getBlobs());
        newCommit.setSecondParent(args[2]);

        // Update add.
        for (String fileName : plainFilenamesIn(Repository.STAGE_ADD_DIR)) {
            if (newCommit.getBlobs().containsValue(fileName)) {
                newCommit.removeBlob(fileName);
            }
            newCommit.addBlob(join(Repository.STAGE_ADD_DIR, fileName));
        }

        clearDir(Repository.STAGE_ADD_DIR);

        // Update remove.
        for (String fileName : plainFilenamesIn(Repository.STAGE_RM_DIR)) {
            if (newCommit.getBlobs().containsValue(fileName)) {
                newCommit.removeBlob(fileName);
            }
        }

        clearDir(Repository.STAGE_RM_DIR);

        newCommit.saveCommit();

        // Update Head, Main or other branch, CommitEditMsg.
        Repository.updateBranch(Repository.getHead(), newCommit.getSha1());
        Repository.writeCommitEditMsg(newCommit.getMessage());

        return;

    }


    /** Unstage the file if it is currently staged for addition. If the file is
     * tracked in the current commit, stage it for removal and remove the file from
     * the working directory if the user has not already done so (do not remove it
     * unless it is tracked in the current commit). **/
    public static void rm(String[] args) {
        validateNumArgs("rm", args, 2);

        File copyAdd = join(Repository.STAGE_ADD_DIR, args[1]);
        File copyRm = join(Repository.STAGE_RM_DIR, args[1]);
        File file = join(Repository.CWD, args[1]);

        // Failure cases: If the file is neither staged nor tracked by the head
        // commit, print the error message No reason to remove the file.
        if (!copyAdd.exists()
                && !Commit.fromCommitFile(Repository.getFetchHead()).
                getBlobs().containsValue(file.getName())) {
            exitWithError("No reason to remove the file.");
        }

        // Unstage the file if it is currently staged for addition.
        if (copyAdd.exists()) {
            copyAdd.delete();
            return;
        }

        // If the file is tracked in the current commit
        // Stage this file to STAGE_RM_DIR.
        if (Commit.fromCommitFile(Repository.getFetchHead()).
                getBlobs().containsValue(file.getName())) {
            writeContents(copyRm, "");
            if (file.exists()) {
                writeContents(copyRm, readContents(file));
                file.delete();
            }
            return;
        }

    }


    /** Starting at the current head commit, display information about each commit
     * backwards along the commit tree until the initial commit, following the first
     * parent commit links, ignoring any second parents found in merge commits. This
     * set of commit nodes is called the commit’s history. For every node in this
     * history, the information it should display is the commit id, the time the
     * commit was made, and the commit message. **/
    public void log(String[] args) {

        // None failure cases.

        validateNumArgs("log", args, 1);

        String parentSha1 = Repository.getFetchHead();
        while (parentSha1 != null) {
            Commit cur = Commit.fromCommitFile(parentSha1);
            parentSha1 = cur.getParent();
            System.out.println("===\n" + cur.toString() + "\n");
        }

        return;

    }


    /** Like log, except displays information about all commits ever made. The order
     *  of the commits does not matter. Hint: there is a useful method in gitlite.
     *  Utils that will help you iterate over files within a directory. **/
    public static void globalLog(String[] args) {
        validateNumArgs("globalLog", args, 1);

        List<String> commitsSha1s = plainFilenamesIn(Repository.COMMITS_DIR);
        for (String sha1 : commitsSha1s) {
            System.out.println("===\n" + Commit.fromCommitFile(sha1).toString() + "\n");
        }

        return;
    }


    /** Prints out the ids of all commits that have the given commit message, one
     * per line. If there are multiple such commits, it prints the ids out on
     * separate lines. The commit message is a single operand; to indicate a
     * multiword message, put the operand in quotation marks, as for the commit
     * command below. Hint: the hint for this command is the same as the one for
     * global-log. **/
    public static void find(String[] args) {
        validateNumArgs("find", args, 2);

        Boolean isFound = false;

        List<String> commitsSha1s = plainFilenamesIn(Repository.COMMITS_DIR);
        for (String sha1 : commitsSha1s) {
            Commit c = Commit.fromCommitFile(sha1);
            if (c.getMessage().equals(args[1])) {
                System.out.println(c.getSha1());
                isFound = true;
            }
        }

        if (!isFound) {
            System.out.println("Found no commit with that message.");
        }

        return;
    }


    /** Displays what branches currently exist, and marks the current branch with
     * a *. Also displays what files have been staged for addition or removal. An
     * example of the exact format it should follow is as follows. **/
    public static void status(String[] args) {
        validateNumArgs("status", args, 1);

        System.out.println("=== Branches ===");
        List<String> branchNames = plainFilenamesIn(Repository.BRANCH_DIR);
        String headName = Repository.getHead();
        for (String branchName : branchNames) {
            if (branchName.equals(headName)) {
                System.out.println("*" + branchName);
            } else {
                System.out.println(branchName);
            }
        }
        System.out.print("\n");

        System.out.println("=== Staged Files ===");
        List<String> addFileNames = plainFilenamesIn(Repository.STAGE_ADD_DIR);
        for (String addFileName : addFileNames) {
            System.out.println(addFileName);
        }
        System.out.print("\n");

        System.out.println("=== Removed Files ===");
        List<String> rmFileNames = plainFilenamesIn(Repository.STAGE_RM_DIR);
        for (String rmFileName : rmFileNames) {
            System.out.println(rmFileName);
        }
        System.out.print("\n");

        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.print("\n");

        System.out.println("=== Untracked Files ===");
        System.out.print("\n");
    }


    /** Checkout is a kind of general command that can do a few different things
     *  depending on what its arguments are. There are 3 possible use cases. In each
     *  section below, you’ll see 3 numbered points. Each corresponds to the
     *  respective usage of checkout. **/
    public static void checkout(String[] args) {
        if (args.length == 3 && args[1].equals("--")) {
            checkout1(args[2]);
        } else if (args.length == 4 && args[2].equals("--")) {
            checkout2(args[1], args[3]);
        } else if (args.length == 2) {
            checkout3(args[1]);
        } else {
            exitWithError("Incorrect operands.");
        }
    }
    /** Takes the version of the file as it exists in the head commit and puts
     *  it in the working directory, overwriting the version of the file
     *  that’s already there if there is one. The new version of the file is
     *  not staged. **/
    public static void checkout1(String fileName) {

        checkout2(Repository.getFetchHead(), fileName);

    }

    /** Takes the version of the file as it exists in the commit with the
     * given id, and puts it in the working directory, overwriting the
     * version of the file that’s already there if there is one. The new
     * version of the file is not staged. **/
    public static void checkout2(String commitID, String fileName) {

        String thatCommitID = commitID;
        // If no commit with the given id exists, print No commit with that
        // id exists. Otherwise, if the file does not exist in the given
        // commit, print the same message as for failure case 1. Do not
        // change the CWD.
        if (commitID.length() >= SHA1LEN) {
            if (!plainFilenamesIn(Repository.COMMITS_DIR).contains(commitID)) {
                exitWithError("No commit with that id exists.");
            }
        } else {
            for (String sha1 : plainFilenamesIn(Repository.COMMITS_DIR)) {
                if (sha1.substring(0, commitID.length()).equals(commitID)) {
                    thatCommitID = sha1;
                    break;
                }
            }
            if (thatCommitID.length() < SHA1LEN) {
                exitWithError("No commit with that id exists.");
            }
        }


        Commit thatCommit = Commit.fromCommitFile(thatCommitID);
        if (!thatCommit.getBlobs().containsValue(fileName)) {
            exitWithError("File does not exist in that commit.");
        }


        File file = join(Repository.CWD, fileName);
        for (String sha1 : thatCommit.getBlobs().keySet()) {
            if (thatCommit.getBlobs().get(sha1).equals(fileName)) {
                File f = join(Repository.BLOBS_DIR, sha1);
                writeContents(file, readContents(f));
                return;
            }
        }

        return;

    }

    /** Takes all files in the commit at the head of the given branch,
     * and puts them in the working directory, overwriting the versions
     * of the files that are already there if they exist. Also, at the
     * end of this command, the given branch will now be considered the
     * current branch (HEAD). Any files that are tracked in the current
     * branch but are not present in the checked-out branch are deleted.
     * The staging area is cleared, unless the checked-out branch is the
     * current branch (see Failure cases below). **/
    public static void checkout3(String branchName) {
        // If no branch with that name exists, print No such branch exists.
        if (!plainFilenamesIn(Repository.BRANCH_DIR).contains(branchName)) {
            exitWithError("No such branch exists.");
        }

        // If that branch is the current branch, print No need to checkout the current branch.
        if (branchName.equals(Repository.getHead())) {
            exitWithError("No need to checkout the current branch.");
        }

        checkout4(branchName, readContentsAsString(join(Repository.BRANCH_DIR, branchName)));

    }

    public static void checkout4(String branchName, String sha1) {
        // If a working file is untracked in the current branch and would be overwritten by the
        // reset, print There is an untracked file in the way; delete it, or add and commit it
        // first. and exit;
        for (String fileName : plainFilenamesIn(Repository.CWD)) {
            if (!Commit.fromCommitFile(Repository.getFetchHead()).
                    getBlobs().containsValue(fileName)
                    && Commit.fromCommitFile(sha1).getBlobs().containsValue(fileName)) {
                exitWithError("There is an untracked file in the way; delete it,"
                        + " or add and commit it first.");
            }
        }

        for (String fileName : plainFilenamesIn(Repository.CWD)) {
            if (Commit.fromCommitFile(sha1).getBlobs().values().contains(fileName)) {
                checkout2(sha1, fileName);
            } else if (Commit.fromCommitFile(Repository.getFetchHead()).
                    getBlobs().containsValue(fileName)) {
                join(Repository.CWD, fileName).delete();
            }
        }

        for (String fileName : Commit.fromCommitFile(sha1).getBlobs().values()) {
            checkout2(sha1, fileName);
        }

        Repository.updateBranch(branchName, sha1);

        clearDir(Repository.STAGE_ADD_DIR);
        clearDir(Repository.STAGE_RM_DIR);

        return;
    }


    /** Creates a new branch with the given name, and points it at the current head
     *  commit. A branch is nothing more than a name for a reference (a SHA-1
     *  identifier) to a commit node. This command does NOT immediately switch to
     *  the newly created branch (just as in real Git). Before you ever call branch,
     *  your code should be running with a default branch called “main”. **/
    public static void branch(String[] args) {
        validateNumArgs("branch", args, 2);

        // If a branch with the given name already exists, print the error message
        // A branch with that name already exists.
        if (join(Repository.BRANCH_DIR, args[1]).exists()) {
            exitWithError("A branch with that name already exists.");
        }

        writeContents(join(Repository.BRANCH_DIR, args[1]), Repository.getFetchHead());
    }


    /** Deletes the branch with the given name. This only means to delete the
     * pointer associated with the branch; it does not mean to delete all commits
     * that were created under the branch, or anything like that. **/
    public static void rmBranch(String[] args) {
        validateNumArgs("rmBranch", args, 2);

        // If a branch with the given name does not exist, aborts. Print the error message
        // A branch with that name does not exist.
        if (!join(Repository.BRANCH_DIR, args[1]).exists()) {
            exitWithError("A branch with that name does not exist.");
        }

        //  If you try to remove the branch you’re currently on, aborts, printing the error
        //  message Cannot remove the current branch.
        if (args[1].equals(Repository.getHead())) {
            exitWithError("Cannot remove the current branch.");
        }

        join(Repository.BRANCH_DIR, args[1]).delete();
        return;

    }


    /** Checks out all the files tracked by the given commit. Removes tracked files
     *  that are not present in that commit. Also moves the current branch’s head to
     *  that commit node. See the intro for an example of what happens to the head
     *  pointer after using reset. The [commit id] may be abbreviated as for
     *  checkout. The staging area is cleared. The command is essentially checkout
     *  of an arbitrary commit that also changes the current branch head. **/
    public static void reset(String[] args) {
        validateNumArgs("reset", args, 2);

        // If no commit with the given id exists, print No commit with that id exists.
        if (!plainFilenamesIn(Repository.COMMITS_DIR).contains(args[1])) {
            exitWithError("No commit with that id exists.");
        }

        checkout4(Repository.getHead(), args[1]);

    }


    /** Depending on how flexibly you have designed the rest of the project, the 3
     *  points (~10% of the final submission points) may not be worth the amount of
     *  effort it takes to do this section. We’re certainly not expecting everyone
     *  to do it. Our priority will be in helping students complete the main project;
     *  if you’re doing this last section, we expect you to be able to stand on your
     *  own a little bit more than most students. **/
    public static void merge(String[] args) {
        validateNumArgs("merge", args, 2);

        // Failure cases
        if (plainFilenamesIn(Repository.STAGE_ADD_DIR).size() != 0
                || plainFilenamesIn(Repository.STAGE_RM_DIR).size() != 0) {
            exitWithError("You have uncommitted changes.");
        }

        if (!join(Repository.BRANCH_DIR, args[1]).exists()) {
            exitWithError("A branch with that name does not exist.");
        }

        if (args[1].equals(Repository.getHead())) {
            exitWithError("Cannot merge a branch with itself.");
        }

        for (String fileName : plainFilenamesIn(Repository.CWD)) {
            if (!Commit.fromCommitFile(Repository.getFetchHead()).
                    getBlobs().containsValue(fileName)) {
                exitWithError("There is an untracked file in the way; delete it,"
                        + " or add and commit it first.");
            }
        }

        String givenBranchSha1 = readContentsAsString(join(Repository.BRANCH_DIR, args[1]));
        String givenBranchName = args[1];
        String currentBranchName = Repository.getHead();
        String currentBranchSha1 = Repository.getFetchHead();

        // special cases
        String parentSha1 = new String(currentBranchSha1);
        while (parentSha1 != null) {
            if (parentSha1.equals(givenBranchSha1)) {
                exitWithError("Given branch is an ancestor of the current branch.");
            }
            parentSha1 = Commit.fromCommitFile(parentSha1).getParent();
        }

        parentSha1 = new String(givenBranchSha1);
        while (parentSha1 != null) {
            if (parentSha1.equals(currentBranchSha1)) {
                checkout3(givenBranchName);
                exitWithError("Current branch fast-forwarded.");
            }
            parentSha1 = Commit.fromCommitFile(parentSha1).getParent();
        }


        String splitSha1 = mergeHelperGetSplitCommit(givenBranchSha1, currentBranchSha1);

        Tuple myTuple = mergeHelperAllFileNames(splitSha1, currentBranchSha1, givenBranchSha1);
        HashMap<String, String> contentsInSplit = myTuple.getContentsInSplit();
        HashMap<String, String> contentsInHead = myTuple.getContentsInHead();
        HashMap<String, String> contentsInOther = myTuple.getContentsInOther();
        HashSet<String> allFileNames = myTuple.getAllFileNames();

        String split;
        String head;
        String other;

        for (String fileName : allFileNames) {
            split = contentsInSplit.get(fileName);
            head = contentsInHead.get(fileName);
            other = contentsInOther.get(fileName);
            mergeHelperConditions(fileName, split, head, other);
        }

        String msg = String.format("Merged %s into %s.", givenBranchName, currentBranchName);
        commitWithSecondParent(new String[]{"commit", msg, givenBranchSha1});

        return;
    }

    /** Get split commit. **/
    public static String mergeHelperGetSplitCommit(String givenBranchSha1,
                                                   String currentBranchSha1) {
        String p1 = new String(givenBranchSha1);
        String p2 = new String(currentBranchSha1);
        while (!p1.equals(p2)) {
            if (Commit.fromCommitFile(p1).getSecondParent() != null) {
                p1 = Commit.fromCommitFile(p1).getSecondParent();
            } else {
                p1 = Commit.fromCommitFile(p1).getParent();
            }

            if (Commit.fromCommitFile(p2).getSecondParent() != null) {
                p2 = Commit.fromCommitFile(p2).getSecondParent();
            } else {
                p2 = Commit.fromCommitFile(p2).getParent();
            }

            if (p1 == null) {
                p1 = new String(currentBranchSha1);
            }
            if (p2 == null) {
                p2 = new String(givenBranchSha1);
            }
        }
        return p1;
    }

    private static class Tuple {
        private HashMap<String, String> contentsInSplit;
        private HashMap<String, String> contentsInHead;
        private HashMap<String, String> contentsInOther;
        private HashSet<String> allFileNames;

        Tuple(HashMap<String, String> contentsInSplit,
                     HashMap<String, String> contentsInHead,
                     HashMap<String, String> contentsInOther,
                     HashSet<String> allFileNames) {
            this.contentsInSplit = contentsInSplit;
            this.contentsInHead = contentsInHead;
            this.contentsInOther = contentsInOther;
            this.allFileNames = allFileNames;
        }
        public HashMap<String, String> getContentsInSplit() {
            return contentsInSplit;
        }

        public HashMap<String, String> getContentsInHead() {
            return contentsInHead;
        }

        public HashMap<String, String> getContentsInOther() {
            return contentsInOther;
        }

        public HashSet<String> getAllFileNames() {
            return allFileNames;
        }
    }
    public static Tuple mergeHelperAllFileNames(String splitSha1,
                                                String currentBranchSha1,
                                                String givenBranchSha1) {
        HashMap<String, String> theSplit =
                Commit.fromCommitFile(splitSha1).getBlobs();
        HashMap<String, String> theHead =
                Commit.fromCommitFile(currentBranchSha1).getBlobs();
        HashMap<String, String> theOther =
                Commit.fromCommitFile(givenBranchSha1).getBlobs();

        HashMap<String, String> contentsInSplit = new HashMap<>();
        HashSet<String> allFileNames = new HashSet<>();
        for (String blobSha1 : theSplit.keySet()) {
            contentsInSplit.put(theSplit.get(blobSha1), blobSha1);
            allFileNames.add(theSplit.get(blobSha1));
        }
        HashMap<String, String> contentsInHead = new HashMap<>();
        for (String blobSha1 : theHead.keySet()) {
            contentsInHead.put(theHead.get(blobSha1), blobSha1);
            allFileNames.add(theHead.get(blobSha1));
        }
        HashMap<String, String> contentsInOther = new HashMap<>();
        for (String blobSha1 : theOther.keySet()) {
            contentsInOther.put(theOther.get(blobSha1), blobSha1);
            allFileNames.add(theOther.get(blobSha1));
        }

        return new Tuple(contentsInSplit, contentsInHead, contentsInOther, allFileNames);
    }

    public static void mergeHelperConditions(String fileName, String split,
                                             String head, String other) {
        // 1
        if (split != null && head != null && other != null
                && split.equals(head) && !split.equals(other)) {
            writeContents(join(Repository.CWD, fileName),
                    readContents(join(Repository.BLOBS_DIR, other)));
            add(new String[]{"add", fileName});
            // 2
        } else if (split != null && head != null && other != null
                && !split.equals(head) && split.equals(other)) {
            return;
            // 3
        } else if (split != null && head != null && other != null
                && !split.equals(head) && !split.equals(other)
                && head.equals(other)) {
            return;
            // 4
        } else if (split == null && head != null && other == null) {
            return;
            // 5
        } else if (split == null && head == null && other != null) {
            writeContents(join(Repository.CWD, fileName),
                    readContents(join(Repository.BLOBS_DIR, other)));
            add(new String[]{"add", fileName});
            // 6
        } else if (split != null && split.equals(head) && other == null) {
            rm(new String[]{"rm", fileName});
            // 7
        } else if (split != null && head == null && split.equals(other)) {
            return;
            // 8
        } else if ((split != null && !split.equals(head) && !split.equals(other)
                && (head != null && !head.equals(other)) || (other != null
                && !other.equals(head))) || (split == null && head != null
                && other != null && !head.equals(other))) {
            String content;
            if (head == null) {
                content = "<<<<<<< HEAD\n"
                        + "=======\n"
                        + readContentsAsString(join(Repository.BLOBS_DIR, other))
                        + ">>>>>>>\n";
            } else if (other == null) {
                content = "<<<<<<< HEAD\n"
                        + readContentsAsString(join(Repository.BLOBS_DIR, head))
                        + "=======\n"
                        + ">>>>>>>\n";
            } else {
                content = "<<<<<<< HEAD\n"
                        + readContentsAsString(join(Repository.BLOBS_DIR, head))
                        + "=======\n"
                        + readContentsAsString(join(Repository.BLOBS_DIR, other))
                        + ">>>>>>>\n";
            }

            writeContents(join(Repository.CWD, fileName), content);

            System.out.println("Encountered a merge conflict.");
        }
    }
}
