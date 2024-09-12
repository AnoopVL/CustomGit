import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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
      List<byte[]> entries = new ArrayList<>();

      try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
        for (Path entry : stream) {     
          if (entry.getFileName().toString().equals(".git")) {
              continue;  // Ignore the .git directory
          }

          if (Files.isDirectory(entry)) {
              String treeHash = writeTree(entry);  // Recursively create tree for subdirectories
              byte[] entryBytes = createTreeEntry(treeHash, entry.getFileName().toString(), "40000");  // Mode for directories
                    entries.add(entryBytes);
          } else {
              String blobHash = createBlob(entry);  // Create a blob object for the file
              byte[] entryBytes = createTreeEntry(blobHash, entry.getFileName().toString(), "100644");  // Mode for regular files
                    entries.add(entryBytes);
                }
            }
        }

        // Sort the entries by filename
        entries.sort(Comparator.comparing(Main::getEntryFileName));

        // Concatenate the entries into a single byte array
        byte[] treeData = concatenateEntries(entries);
      
        // Add the tree header
        String header = "tree " + treeData.length + "\0";
        byte[] treeObject = concatenate(header.getBytes(), treeData);
      
        // Write the tree object and return its SHA-1 hash
        return writeObjectToGit(treeObject);
    }
      
          // Create a tree entry in the format <mode> <name>\0<20-byte SHA>
          private static byte[] createTreeEntry(String shaHex, String name, String mode) throws NoSuchAlgorithmException {
              ByteArrayOutputStream entryStream = new ByteArrayOutputStream();
      
              try {
                  // Write the mode and name
                  entryStream.write((mode + " " + name).getBytes());
                  entryStream.write(0);  // Null byte
      
                  // Convert SHA-1 hex to binary and write it
                  entryStream.write(hexToBytes(shaHex));
              } catch (IOException e) {
                  throw new RuntimeException(e);
              }
      
              return entryStream.toByteArray();
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
      
          // Helper to concatenate multiple byte arrays
          private static byte[] concatenate(byte[]... arrays) {
              ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
              for (byte[] array : arrays) {
                  try {
                      outputStream.write(array);
                  } catch (IOException e) {
                      throw new RuntimeException(e);
                  }
              }
              return outputStream.toByteArray();
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
    
          // Helper to sort tree entries by file name
          private static String getEntryFileName(byte[] entry) {
              int endIndex = 0;
              while (entry[endIndex] != 0) {
                  endIndex++;
              }
              return new String(entry, 0, endIndex);
          }
      
          // Concatenate all tree entries into a single byte array
          private static byte[] concatenateEntries(List<byte[]> entries) {
              ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
              for (byte[] entry : entries) {
                  try {
                      outputStream.write(entry);
                  } catch (IOException e) {
                      throw new RuntimeException(e);
                  }
              }
              return outputStream.toByteArray();
          }

}
