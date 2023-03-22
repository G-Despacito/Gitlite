package gitlite;

import static gitlite.Utils.*;

/** Driver class for Gitlite, a subset of the Git version-control system.
 *  @author G-Despacito
 */
public class Main {

    /** Usage: java gitlite.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {

        Bloop bloop = new Bloop();

        if (args == null || args.length == 0) {
            exitWithError("Please enter a command.");
        }

        String firstArg = args[0];
        switch (firstArg) {
            case "init":
                bloop.init(args);
                break;
            case "add":
                validateRepoExists(args);
                bloop.add(args);
                break;
            case "commit":
                validateRepoExists(args);
                bloop.commit(args);
                break;
            case "rm":
                validateRepoExists(args);
                bloop.rm(args);
                break;
            case "log":
                validateRepoExists(args);
                bloop.log(args);
                break;
            case "global-log":
                validateRepoExists(args);
                bloop.globalLog(args);
                break;
            case "find":
                validateRepoExists(args);
                bloop.find(args);
                break;
            case "status":
                validateRepoExists(args);
                bloop.status(args);
                break;
            case "checkout":
                validateRepoExists(args);
                bloop.checkout(args);
                break;
            case "branch":
                validateRepoExists(args);
                bloop.branch(args);
                break;
            case "rm-branch":
                validateRepoExists(args);
                bloop.rmBranch(args);
                break;
            case "reset":
                validateRepoExists(args);
                bloop.reset(args);
                break;
            case "merge":
                validateRepoExists(args);
                bloop.merge(args);
                break;
            default:
                exitWithError("No command with that name exists.");
        }
        return;
    }

}
