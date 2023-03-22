package gitlet;

import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;

import static gitlet.Utils.*;

/** Represents a gitlet commit object.
 *  does at a high level.
 *
 *  @author Xinyi Zhang
 */
public class Commit implements Serializable {

    /** Folder of Commit objects. */
    static final File COMMITS_DIR = Repository.COMMITS_DIR;
    /** Folder of Blob objects. */
    static final File BLOBS_DIR = Repository.BLOBS_DIR;
    /** Format timestamp, for example, as Sat Nov 11 12:30:00 2017 -0800. **/
    static final SimpleDateFormat SIMPLE_DATE_FORMAT
            = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");


    /** The message of this Commit. */
    private String message;
    /** The time at which a commit was created. **/
    private String timestamp;
    /** The parent commit of this Commit. **/
    private String parent;
    /** The second parent commit of this Commit. **/
    private String secondParent;
    /** The blobs that this Commit contains. **/
    private HashMap<String, String> blobs;


    /** The SHA-1 code of this Commit. **/
    private String sha1;


    /** Constructor from only message. **/
    public Commit(String message) {
        this.message = message;
        this.parent = null;
        this.timestamp = SIMPLE_DATE_FORMAT.format(new Date(0));
        this.blobs = new HashMap<>();
    }

    /** Constructor from message and parent. **/
    public Commit(String message, String parent) {
        this.message = message;
        this.parent = parent;
        this.timestamp = SIMPLE_DATE_FORMAT.format(new Date());
        this.blobs = null;
    }

    /** Constructor from message and parent, secondParent. **/
    public Commit(String message, String parent, String secondParent) {
        this.message = message;
        this.parent = parent;
        this.secondParent = secondParent;
        this.timestamp = SIMPLE_DATE_FORMAT.format(new Date());
        this.blobs = null;
    }

    /** Constructor from message, timestamp, parent, blobs, and sha1. **/
    public Commit(String message, String timestamp, String parent, String secondParent,
                  HashMap<String, String> blobs, String sha1) {
        this.message = message;
        this.timestamp = timestamp;
        this.parent = parent;
        this.secondParent = secondParent;
        this.blobs = blobs;
        this.sha1 = sha1;
    }

    /** Get message of this commit. **/
    public String getMessage() {
        return this.message;
    }

    /** Get timestamp of this commit. **/
    public String getTimestamp() {
        return this.timestamp;
    }

    /** Get parent of this commit. **/
    public String getParent() {
        return this.parent;
    }

    /** Get second parent of this commit. **/
    public String getSecondParent() {
        return this.secondParent;
    }

    /** Get SHA-1 of this commit. **/
    public String getSha1() {
        return this.sha1;
    }

    /** Get blobs of this commit. **/
    public HashMap<String, String> getBlobs() {
        return this.blobs;
    }

    /** Set blobs of this commit. **/
    public void setBlobs(HashMap<String, String> blob) {
        this.blobs = blob;
    }

    /** Set second parent of this commit. **/
    public void setSecondParent(String mySha1) {
        this.secondParent = mySha1;
    }

    /** Add blobs of this commit. **/
    public void addBlob(File file) {
        String mySha1 = sha1(file.getName(), readContents(file));
        if (blobs.containsKey(mySha1)) {
            return;
        }
        blobs.put(mySha1, file.getName());
        return;
    }

    /** Remove blob(s) of certain file name from Commit blobs.
     *  If no such file exists, do nothing. **/
    public void removeBlob(String fileName) {
        Iterator<Map.Entry<String, String>> iterator = blobs.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            if (entry.getValue().equals(fileName)) {
                iterator.remove();
            }
        }
    }

    public void generateSha1() {
        ArrayList<String> objList = new ArrayList<>();

        if (message != null) {
            objList.add(message);
        }
        if (timestamp != null) {
            objList.add(timestamp);
        }
        if (parent != null) {
            objList.add(parent);
        }
        if (secondParent != null) {
            objList.add(secondParent);
        }
        if (blobs != null && blobs.size() > 0) {
            objList.add(blobs.toString());
        }

        this.sha1 = sha1(objList.toArray());

        return;
    }


    /**
     * Reads in and deserializes a commit from a file with SHA-1 sha1 in COMMIT_FOLDER.
     *
     * @param sha1 SHA-1 of commit to load
     * @return Commit read from file
     */
    public static Commit fromCommitFile(String sha1) {
        File inFile = Utils.join(COMMITS_DIR, sha1);
        try {
            ObjectInputStream inp = new ObjectInputStream(new FileInputStream(inFile));
            Commit c = (Commit) inp.readObject();
            inp.close();
            return c;
        } catch (IOException | ClassNotFoundException excp) {
            System.out.println(excp.getMessage());
            Commit c = null;
            return c;
        }
    }

    /**
     * Saves a Commit to a file for future use.
     */
    public void saveCommit() {
        generateSha1();
        Commit c = new Commit(message, timestamp, parent, secondParent, blobs, sha1);
        File outFile = Utils.join(COMMITS_DIR, sha1);
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(outFile));
            out.writeObject(c);
            out.close();
        } catch (IOException excp) {
            System.out.println(excp.getMessage());
        }
    }

    /**
     * Saves a Blob to a file for future use.
     */
    public static void saveBlob(File file) {
        String sha1 = sha1(file.getName(), readContents(file));

        File outFile = Utils.join(BLOBS_DIR, sha1);
        writeContents(outFile, readContents(file));
        return;
    }

    @Override
    public String toString() {
        return String.format("commit %s\nDate: %s\n%s",
                sha1, timestamp, message, "\n");
    }


}
