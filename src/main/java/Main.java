import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
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
      default -> System.out.println("Unknown command: " + command);
    }
  }
}
