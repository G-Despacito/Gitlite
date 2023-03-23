package gitlite;

import java.io.File;

import static gitlite.Utils.*;


/** Represents a gitlite repository.
 *
 *  @author Xinyi Zhang
 */
public class Repository {
    /**
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlite directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlite");

    /** The .gitlite.objects directory. **/
    public static final File COMMITS_DIR = join(GITLET_DIR, "commits");
    /** The .gitlite.refs directory. **/
    public static final File BLOBS_DIR = join(GITLET_DIR, "blobs");
    /** The branch directory, stores the sha1 files of all the branches heads. **/
    public static final File BRANCH_DIR = join(GITLET_DIR, "branch");

    /** The HEAD of the repository. Consists of SHA-1 code. **/
    private static final File FETCH_HEAD = join(GITLET_DIR, "fetch_head.txt");
    /** The HEAD of branch main of the repository. Consists of SHA-1 code. **/
    private static final File FETCH_MAIN = join(GITLET_DIR, "fetch_main.txt");

    /** The commit message of the newest commit. **/
    private static final File COMMIT_EDITMSG = join(GITLET_DIR, "commit_editmsg.txt");
    /** The HEAD of the repository. Consists of relative path, for example,
     * ref: refs/heads/main **/
    private static final File HEAD = join(GITLET_DIR, "head.txt");

    /** Staging Area: staged for addition. **/
    public static final File STAGE_ADD_DIR = join(GITLET_DIR, "stage_add");
    /** Staging Area: staged for removal. **/
    public static final File STAGE_RM_DIR = join(GITLET_DIR, "stage_rm");


    /** Constructor. **/
    public Repository() {
        if (!GITLET_DIR.exists()) {
            GITLET_DIR.mkdir();
        }
        if (!COMMITS_DIR.exists()) {
            COMMITS_DIR.mkdir();
        }
        if (!BLOBS_DIR.exists()) {
            BLOBS_DIR.mkdir();
        }
        if (!FETCH_HEAD.exists()) {
            Utils.writeContents(FETCH_HEAD, "");
        }
        if (!FETCH_MAIN.exists()) {
            Utils.writeContents(FETCH_MAIN, "");
        }
        if (!COMMIT_EDITMSG.exists()) {
            Utils.writeContents(COMMIT_EDITMSG, "");
        }
        if (!HEAD.exists()) {
            Utils.writeContents(HEAD, "main");
        }
        if (!STAGE_ADD_DIR.exists()) {
            STAGE_ADD_DIR.mkdir();
        }
        if (!STAGE_RM_DIR.exists()) {
            STAGE_RM_DIR.mkdir();
        }
        if (!BRANCH_DIR.exists()) {
            BRANCH_DIR.mkdir();
        }
    }

    public static void writeFetchHead(String fetchHead) {
        Utils.writeContents(FETCH_HEAD, fetchHead);
    }

    public static void writeFetchMain(String fetchMain) {
        Utils.writeContents(FETCH_MAIN, fetchMain);
    }

    public static void writeHead(String branchName) {
        Utils.writeContents(HEAD, branchName);
        if (!join(BRANCH_DIR, branchName).exists()) {
            writeContents(join(BRANCH_DIR, branchName), "");
        }
    }

    public static void updateBranch(String branchName, String fetchHead) {
        writeHead(branchName);
        writeFetchHead(fetchHead);
        writeContents(join(BRANCH_DIR, readContentsAsString(HEAD)), fetchHead);
        if (readContentsAsString(HEAD).equals("main")) {
            writeFetchMain(fetchHead);
        }
    }

    public static String getFetchHead() {
        return readContentsAsString(FETCH_HEAD);
    }

    public static String getFetchMain() {
        return readContentsAsString(FETCH_MAIN);
    }

    public static String getHead() {
        return readContentsAsString(HEAD);
    }

    public static void writeCommitEditMsg(String commitEditMsg) {
        Utils.writeContents(COMMIT_EDITMSG, commitEditMsg);
    }


}
