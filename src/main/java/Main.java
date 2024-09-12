import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class Main {
  public static void main(String[] args){
    
    final String command = args[0];
    switch (command) {
      case "init" -> {
        final File root = new File(".git");
        new File(root, "objects").mkdirs();
        new File(root, "refs").mkdirs();
        final File head = new File(root, "HEAD");
    
        try {
          head.createNewFile();
          Files.write(head.toPath(), "ref: refs/heads/main\n".getBytes());
          System.out.println("Initialized git directory");
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      case "cat-file" ->{
        if(!"-p".equals(args[1])){
          System.out.println("Incorrect arguments for cat-file");
          return;
        }
        String hash = args[2];
        String dir = hash.substring(0,2);
        hash = hash.substring(2);

        File file = new File("./.git/objects/" + dir + "/" + hash);
        try {
          String blobObj = new BufferedReader(new InputStreamReader(new InflaterInputStream(new FileInputStream(file)))).readLine();
          
          String content = blobObj.substring(blobObj.indexOf('\0') + 1);
          System.out.print(content);
          
        }catch(IOException e){
          throw new RuntimeException(e);
        }
    }

    case "hash-object" -> {
      if (!"-w".equals(args[1])) {
          System.out.println("Incorrect arguments for hash-object");
          return;
      }
      String filePath = args[2];

      try {
          // Step 1: Read file content
          byte[] fileContent = Files.readAllBytes(Paths.get(filePath));

          // Step 2: Prepare the blob header
          String header = "blob " + fileContent.length + "\0";
          byte[] blob = concatenate(header.getBytes(), fileContent);

          // Step 3: Compute SHA-1 hash
          MessageDigest md = MessageDigest.getInstance("SHA-1");
          byte[] sha1Hash = md.digest(blob);
          String sha1Hex = bytesToHex(sha1Hash);

          // Step 4: Write the object to .git/objects
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

          // Step 5: Print the SHA-1 hash
          System.out.println(sha1Hex);

      } catch (IOException | NoSuchAlgorithmException e) {
        throw new RuntimeException(e);
      }
    }

    case "ls-tree" -> {
      if (!"--name-only".equals(args[1])) {
          System.out.println("Incorrect arguments for ls-tree");
          return;
      }
      String treeHash = args[2];
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
  
          // Step 1: Read the header (tree <size>\0), ignore it for now
          while (treeObject[index] != 0) {
              index++;
          }
          index++; // Skip the null byte
  
          List<String> entries = new ArrayList<>();
  
          // Step 2: Parse each entry
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
              index = nameEnd + 21; // Skip the null byte and 20-byte SHA hash (20 bytes)
  
              // Continue reading the next entry
          }
  
          // Step 3: Print the entries in alphabetical order
          Collections.sort(entries);
          for (String entry : entries) {
              System.out.println(entry);
          }
  
      } catch (IOException e) {
          throw new RuntimeException(e);
      }
  }
  

      default -> System.out.println("Unknown command: " + command);
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

}
