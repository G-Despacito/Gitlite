package gitlite;

/** General exception indicating a Gitlite error.  For fatal errors, the
 *  result of .getMessage() is the error message to be printed.
 *  @author P. N. Hilfinger
 */
class GitliteException extends RuntimeException {


    /** A GitliteException with no message. */
    GitliteException() {
        super();
    }

    /** A GitliteException MSG as its message. */
    GitliteException(String msg) {
        super(msg);
    }

}
