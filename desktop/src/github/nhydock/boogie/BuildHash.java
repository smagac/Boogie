package github.nhydock.boogie;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;

/**
 * Simple hash building script for generating boogie.hash files of your game's data.
 * Can be a bit intensive as it reads over files entirely in order to generate a proper, consistent
 * MD5 hash of them for records.
 * @author nhydock
 *
 */
public class BuildHash {

	private static ArrayList<String> files = new ArrayList<String>();
	private static ArrayList<String> paths = new ArrayList<String>();
	
	private static class BuildProcessor implements ApplicationListener {

        private String directory;
        private String output;

        public BuildProcessor(String[] args) {
          //first arg needs to be directory
            directory = args[0];
            output = ".";
            if (args.length > 1)
            {
                output = args[1];
            }
            
            for (String arg : args) {
                System.out.println("args: " + arg);
            }
        }

        @Override
        public void create() {
            FileHandle hash = Gdx.files.absolute(output+"/boogie.hash");
            try{
                System.out.println(hash.file().getAbsolutePath());
                hash.file().createNewFile();
                FileHandle root = Gdx.files.absolute(directory);
                
                crawl(root, root);
                try(Writer writer = hash.writer(false))
                {
                    for (int i = 0; i < paths.size(); i++)
                    {
                        String file = files.get(i);
                        String path = paths.get(i);
                        MessageDigest md = MessageDigest.getInstance("MD5");
                        try (InputStream is = Files.newInputStream(Paths.get(path))) {
                          DigestInputStream dis = new DigestInputStream(is, md);
                          /* Read stream to EOF as normal... */
                          while (dis.read() != -1);
                          
                        }
                        byte[] digest = md.digest();
                        String md5 = bytesToHex(digest);
                        System.out.println("Hashed " + file + " as " + md5);
                        writer.write(md5 + " " + file + "\n");
                    }
                }
            }
            catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            System.out.println("\n\n====================================\nDone hasing!  Results can be found in " + hash.file().getAbsolutePath());   
            
            Gdx.app.exit();
        }

        @Override
        public void resize(int width, int height) {}

        @Override
        public void render() {
            
        }

        @Override
        public void pause() {}

        @Override
        public void resume() {}

        @Override
        public void dispose() {}
        

        /**
         * Crawls all nodes in the directory tree from a starting node, with path names relative to the root node
         * @param root
         * @param root2
         */
        private static void crawl(FileHandle root, FileHandle root2)
        {
            System.out.println(root2.file().getAbsolutePath());
            String path = root.file().getAbsolutePath().substring(root2.file().getAbsolutePath().length() + ((root != root2)?1:0));
            if (root != root2)
                path += "/";
            
            System.out.println(path);
            for (FileHandle f : root.list())
            {
                //ignore hidden files
                if (f.file().isHidden())
                    continue;
                
                //ignore boogie file
                if (f.name().equals("boogie.hash"))
                    continue;
                
                //crawl directory trees
                if (f.isDirectory())
                {
                    crawl(f, root2);
                }
                else
                {
                    System.out.println(path + "/" + f.name());
                    files.add(path + f.name());
                    paths.add(f.file().getAbsolutePath());
                }
            }
        }
        
        final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
        public static String bytesToHex(byte[] bytes) {
            char[] hexChars = new char[bytes.length * 2];
            for ( int j = 0; j < bytes.length; j++ ) {
                int v = bytes[j] & 0xFF;
                hexChars[j * 2] = hexArray[v >>> 4];
                hexChars[j * 2 + 1] = hexArray[v & 0x0F];
            }
            return new String(hexChars);
        }
	    
	}
	
	public static void main(String... args) throws IOException, NoSuchAlgorithmException {
	    HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
	    config.renderInterval = -1f;
	    new HeadlessApplication(new BuildProcessor(args), config);
	}
}
