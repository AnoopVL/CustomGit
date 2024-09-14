import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class Main {
  public static void main(String[] args){
    
    final String command = args[0];
    switch (command) {
      case "init" -> initRepository();
      case "cat-file" -> {
          if (!"-p".equals(args[1])) {
            System.out.println("Incorrect arguments for cat-file");
            return;
          }
        catFile(args[2]);
      }
      case "hash-object" -> {
        if (!"-w".equals(args[1])) {
          System.out.println("Incorrect arguments for hash-object");
          return;
        }
        hashObject(args[2]);
      }

    case "ls-tree" -> {
      if (!"--name-only".equals(args[1])) {
          System.out.println("Incorrect arguments for ls-tree");
          return;
      }
      lsTree(args[2]);
    }

    case "write-tree" -> {
      try {
          String treeHash = writeTree(Paths.get("."));  // Start from the current directory
          System.out.println(treeHash);
      } catch (IOException | NoSuchAlgorithmException e) {
          throw new RuntimeException(e);
      }
    }

    case "commit-tree" -> {
        if (args.length < 4 || !args[2].equals("-m")) {
            System.out.println("Usage: commit-tree <tree-sha> -m <message> [-p <parent-commit>]");
            return;
        }
        String treeSha = args[1];
        String message = args[3];
        String parentSha = args.length > 5 && args[4].equals("-p") ? args[5] : null;
        try {
            String commitHash = commitTree(treeSha, parentSha, message);
            System.out.println(commitHash);
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    case "clone" ->{
        if (args.length < 3 || !args[0].equals("clone")) {
            System.out.println("Usage: java Main clone <repo_url> <target_directory>");
            return;
        }

        String repoUrl = args[1];
        String targetDirectory = args[2];

        try {
            cloneRepository(repoUrl, targetDirectory);
        } catch (Exception e) {
            System.err.println("Error cloning repository: " + e.getMessage());
            e.printStackTrace();
        }
    }

      default -> System.out.println("Unknown command: " + command);
    }
  }

   // Initialize a new repository
   private static void initRepository() {
    File root = new File(".git");
    new File(root, "objects").mkdirs();
    new File(root, "refs").mkdirs();

    File head = new File(root, "HEAD");
    try {
        head.createNewFile();
        Files.write(head.toPath(), "ref: refs/heads/main\n".getBytes());
        System.out.println("Initialized git directory");
    } catch (IOException e) {
        throw new RuntimeException(e);
    }
}

// Print the content of the file object based on its hash
private static void catFile(String hash) {
    String dir = hash.substring(0, 2);
    String fileHash = hash.substring(2);
    File file = new File(".git/objects/" + dir + "/" + fileHash);

    try (InflaterInputStream inflater = new InflaterInputStream(new FileInputStream(file));
         BufferedReader reader = new BufferedReader(new InputStreamReader(inflater))) {
        String blobObj = reader.readLine();
        if (blobObj != null) {
            String content = blobObj.substring(blobObj.indexOf('\0') + 1);
            System.out.print(content);
        }
    } catch (IOException e) {
        throw new RuntimeException(e);
    }
}


  // Helper method to concatenate byte arrays
  private static byte[] concatenate(byte[] header, byte[] content) {
    byte[] result = new byte[header.length + content.length];
    System.arraycopy(header, 0, result, 0, header.length);
    System.arraycopy(content, 0, result, header.length, content.length);
    return result;
  }

  // Helper method to convert byte array to hex string
  private static String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
        sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  // Hash the object and store it in the .git/objects directory
  private static void hashObject(String filePath) {
    try {
        byte[] fileContent = Files.readAllBytes(Paths.get(filePath));

        // Prepare the blob header
        String header = "blob " + fileContent.length + "\0";
        byte[] blob = concatenate(header.getBytes(), fileContent);

        // Compute SHA-1 hash
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = md.digest(blob);
        String sha1Hex = bytesToHex(sha1Hash);

        // Write the object to the .git/objects directory
        String dir = sha1Hex.substring(0, 2);
        String fileName = sha1Hex.substring(2);
        File objectDir = new File(".git/objects/" + dir);
        if (!objectDir.exists()) {
            objectDir.mkdirs();
        }
        File objectFile = new File(objectDir, fileName);

        try (FileOutputStream fos = new FileOutputStream(objectFile);
             DeflaterOutputStream dos = new DeflaterOutputStream(fos)) {
            dos.write(blob);
        }

        // Print the SHA-1 hash
        System.out.println(sha1Hex);

    } catch (IOException | NoSuchAlgorithmException e) {
        throw new RuntimeException(e);
    }
}

  private static void lsTree(String treeHash) {
        String dir = treeHash.substring(0, 2);
        String hash = treeHash.substring(2);
        File file = new File(".git/objects/" + dir + "/" + hash);

        try (InflaterInputStream inflaterInputStream = new InflaterInputStream(new FileInputStream(file))) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inflaterInputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, len);
            }

            byte[] treeObject = byteArrayOutputStream.toByteArray();
            int index = 0;

            // Skip the header (tree <size>\0)
            while (treeObject[index] != 0) {
                index++;
            }
            index++; // Skip the null byte

            List<String> entries = new ArrayList<>();

            // Parse each entry
            while (index < treeObject.length) {
                // Extract mode (file type)
                int modeEnd = index;
                while (treeObject[modeEnd] != ' ') modeEnd++;
                String mode = new String(treeObject, index, modeEnd - index);
                index = modeEnd + 1;

                // Extract name (file/directory name)
                int nameEnd = index;
                while (treeObject[nameEnd] != 0) nameEnd++;
                String name = new String(treeObject, index, nameEnd - index);
                entries.add(name); // Add name to the list
                index = nameEnd + 21; // Skip the null byte and 20-byte SHA hash

                // Continue to the next entry
            }

            // Print the entries in alphabetical order
            Collections.sort(entries);
            for (String entry : entries) {
                System.out.println(entry);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Write the tree object for the current directory
    private static String writeTree(Path directory) throws IOException, NoSuchAlgorithmException {
      List<TreeEntry> entries = new ArrayList<>();

      try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
        for (Path entry : stream) {     
            if (entry.getFileName().toString().equals(".git")) {
                continue;  // Ignore the .git directory
            }

            if (Files.isDirectory(entry)) {
                String treeHash = writeTree(entry);  // Recursively create tree for subdirectories
                entries.add(new TreeEntry("40000", entry.getFileName().toString(), treeHash));
            } else {
                String blobHash = createBlob(entry);  // Create a blob object for the file
                entries.add(new TreeEntry("100644", entry.getFileName().toString(), blobHash));
            }
        }
      } 

      // Sort the entries by filename
      Collections.sort(entries);

      // Concatenate the entries into a single byte array
      ByteArrayOutputStream treeContent = new ByteArrayOutputStream();
      for (TreeEntry entry : entries) {
        treeContent.write(entry.getBytes());
      }
      byte[] treeData = treeContent.toByteArray();

      // Add the tree header
      String header = "tree " + treeData.length + "\0";
      byte[] treeObject = concatenate(header.getBytes(), treeData);

      // Write the tree object and return its SHA-1 hash
      return writeObjectToGit(treeObject);
    }

  // Helper class to represent a tree entry
private static class TreeEntry implements Comparable<TreeEntry> {
  String mode;
  String name;
  String hash;

  TreeEntry(String mode, String name, String hash) {
      this.mode = mode;
      this.name = name;
      this.hash = hash;
  }

  byte[] getBytes() throws IOException {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      out.write((mode + " " + name + "\0").getBytes());
      out.write(hexToBytes(hash));
      return out.toByteArray();
  }

  @Override
  public int compareTo(TreeEntry other) {
      return this.name.compareTo(other.name);
  }
}
        
      
    // Create a blob object for the given file and return its SHA-1 hash
    private static String createBlob(Path file) throws IOException, NoSuchAlgorithmException {
        byte[] fileContent = Files.readAllBytes(file);
        String header = "blob " + fileContent.length + "\0";
        byte[] blob = concatenate(header.getBytes(), fileContent);
        return writeObjectToGit(blob);  // Write the blob object and return its hash
    }
      
    // Write a git object (blob/tree) to the .git/objects directory and return its SHA-1 hash
    private static String writeObjectToGit(byte[] object) throws NoSuchAlgorithmException, IOException {
        // Compute the SHA-1 hash of the object
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = md.digest(object);
        String sha1Hex = bytesToHex(sha1Hash);

        // Write the object to the .git/objects directory
        String dir = sha1Hex.substring(0, 2);
        String fileName = sha1Hex.substring(2);
        File objectDir = new File(".git/objects/" + dir);
        if (!objectDir.exists()) {
            objectDir.mkdirs();
        }
        File objectFile = new File(objectDir, fileName);

        try (FileOutputStream fos = new FileOutputStream(objectFile);
             DeflaterOutputStream dos = new DeflaterOutputStream(fos)) {
            dos.write(object);
        }

        return sha1Hex;
    }

    private static String commitTree(String treeSha, String parentSha, String message) throws IOException, NoSuchAlgorithmException {
        ByteArrayOutputStream commitContent = new ByteArrayOutputStream();

        // Write tree
        commitContent.write(("tree " + treeSha + "\n").getBytes(StandardCharsets.UTF_8));

        // Write parent if provided
        if (parentSha != null) {
            commitContent.write(("parent " + parentSha + "\n").getBytes(StandardCharsets.UTF_8));
        }

        // Write author and committer (using hardcoded values)
        String timestamp = getFormattedTimestamp();
        String authorLine = "author Anoop Lanjekar <avlanjekar4@gmail.com.com> " + timestamp + "\n";
        String committerLine = "committer Anoop Lanjekar <avlanjekar4@gmail.com> " + timestamp + "\n";
        commitContent.write(authorLine.getBytes(StandardCharsets.UTF_8));
        commitContent.write(committerLine.getBytes(StandardCharsets.UTF_8));

        // Write an empty line
        commitContent.write("\n".getBytes(StandardCharsets.UTF_8));

        // Write commit message
        commitContent.write((message + "\n").getBytes(StandardCharsets.UTF_8));

        // Prepare the commit object
        byte[] commitBytes = commitContent.toByteArray();
        String header = "commit " + commitBytes.length + "\0";
        byte[] commitObject = concatenate(header.getBytes(StandardCharsets.UTF_8), commitBytes);

        // Write the commit object and return its SHA-1 hash
        return writeObjectToGit(commitObject);
    }

    private static String getFormattedTimestamp() {
        Instant now = Instant.now();
        String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneOffset.UTC)
                .format(now);
        int offset = ZoneOffset.systemDefault().getRules().getOffset(now).getTotalSeconds() / 60;
        String offsetStr = String.format("%+05d", offset / 60 * 100 + offset % 60);
        return timestamp + " " + offsetStr;
    }

    // Clone the repo
    private static void cloneRepository(String repoUrl, String targetDirectory) throws IOException {
        Path targetDir = Paths.get(targetDirectory);
        Files.createDirectories(targetDir.resolve(".git/objects"));
        Files.createDirectories(targetDir.resolve(".git/refs/heads"));
        Files.write(targetDir.resolve(".git/HEAD"), "ref: refs/heads/master".getBytes());

        String commitHash = getLatestCommitHash(repoUrl);
        System.out.println("Latest commit hash: " + commitHash);

        fetchObjects(repoUrl, targetDirectory, commitHash);

        System.out.println("Repository cloned to: " + targetDir.toAbsolutePath());
    }

    private static String getLatestCommitHash(String repoUrl) throws IOException {
        URL url = new URL(repoUrl + "/info/refs?service=git-upload-pack");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            Pattern pattern = Pattern.compile("([a-f0-9]{40}) refs/heads/(master|main)");
            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        }
        throw new IOException("Could not find the latest commit hash for the master or main branch");
    }

    private static void fetchObjects(String repoUrl, String targetDirectory, String commitHash) throws IOException {
        URL url = new URL(repoUrl + "/git-upload-pack");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-git-upload-pack-request");

        try (OutputStream out = conn.getOutputStream()) {
            String request = String.format("0032want %s\n00000009done\n", commitHash);
            out.write(request.getBytes());
        }

        try (InputStream in = conn.getInputStream();
             FileOutputStream fos = new FileOutputStream(targetDirectory + "/.git/objects/pack")) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }

        // TODO: Implement pack file parsing and object extraction
        System.out.println("Pack file downloaded. Parsing and object extraction not yet implemented.");
    }

    private static void initBareRepository(String targetDir) throws IOException {
        Files.createDirectories(Paths.get(targetDir, ".git", "objects"));
        Files.createDirectories(Paths.get(targetDir, ".git", "refs"));
        Files.write(Paths.get(targetDir, ".git", "HEAD"), "ref: refs/heads/main".getBytes());
    }

    @SuppressWarnings("deprecation")
    private static String sendSmartHttpRequest(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestProperty("Git-Protocol", "version=2");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }
            return response.toString();
        }
    }

    private static byte[] requestPackfile(String repoUrl, String wantRef) throws IOException {
        @SuppressWarnings("deprecation")
        URL url = new URL(repoUrl + "/git-upload-pack");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        String request = "0032want " + wantRef + "\n00000009done\n";
        try (OutputStream os = conn.getOutputStream()) {
            os.write(request.getBytes());
        }

        try (InputStream is = conn.getInputStream()) {
            return is.readAllBytes();
        }
    }

    private static void processPackfile(byte[] packfile, String targetDir) throws IOException, NoSuchAlgorithmException {
        try (InputStream is = new ByteArrayInputStream(packfile)) {
            // Skip the header
            is.skip(8);

            int objectCount = readInt32(is);
            for (int i = 0; i < objectCount; i++) {
                int objectType = (is.read() >> 4) & 7;
                long size = readVariableLengthInteger(is);

                byte[] objectData = new byte[(int) size];
                is.read(objectData);

                switch (objectType) {
                    case 1: // Commit
                    case 2: // Tree
                    case 3: // Blob
                        writeObject(targetDir, objectType, objectData);
                        break;
                    // Handle other object types if needed
                }
            }
        }
    }

    private static int readInt32(InputStream is) throws IOException {
        byte[] buf = new byte[4];
        is.read(buf);
        return ((buf[0] & 0xFF) << 24) | ((buf[1] & 0xFF) << 16) | ((buf[2] & 0xFF) << 8) | (buf[3] & 0xFF);
    }

    private static long readVariableLengthInteger(InputStream is) throws IOException {
        long value = 0;
        int shift = 0;
        int b;
        do {
            b = is.read();
            value |= (long) (b & 0x7F) << shift;
            shift += 7;
        } while ((b & 0x80) != 0);
        return value;
    }

    private static void writeObject(String targetDir, int objectType, byte[] data) throws IOException, NoSuchAlgorithmException {
        String type = switch (objectType) {
            case 1 -> "commit";
            case 2 -> "tree";
            case 3 -> "blob";
            default -> throw new IllegalArgumentException("Unknown object type: " + objectType);
        };

        byte[] header = (type + " " + data.length + "\0").getBytes();
        byte[] fullObject = new byte[header.length + data.length];
        System.arraycopy(header, 0, fullObject, 0, header.length);
        System.arraycopy(data, 0, fullObject, header.length, data.length);

        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] sha1 = md.digest(fullObject);
        String hash = bytesToHex(sha1);

        String objectDir = targetDir + "/.git/objects/" + hash.substring(0, 2);
        Files.createDirectories(Paths.get(objectDir));
        String objectPath = objectDir + "/" + hash.substring(2);

        try (OutputStream os = new DeflaterOutputStream(new FileOutputStream(objectPath))) {
            os.write(fullObject);
        }
    }

    // Convert SHA-1 hex string to binary
    private static byte[] hexToBytes(String shaHex) {
        int len = shaHex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(shaHex.charAt(i), 16) << 4)
                    + Character.digit(shaHex.charAt(i + 1), 16));
        }
        return data;
    }
    

}
