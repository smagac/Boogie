package github.nhydock.boogie;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

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
	
	public static void main(String... args) throws IOException, NoSuchAlgorithmException {
		
		//first arg needs to be directory
		String directory = args[0];
		String output = "";
		if (args.length > 1)
		{
			output = args[1];
		}
		
		File hash = new File(output+"boogie.hash");
		hash.createNewFile();
		File root = new File(directory);
		
		crawl(root, root);
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(hash)))
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
				writer.write(md5 + " " + file);
				writer.newLine();
			}
		}
		System.out.println("\n\n====================================\nDone hasing!  Results can be found in " + hash.getAbsolutePath());
	}
	
	/**
	 * Crawls all nodes in the directory tree from a starting node, with path names relative to the root node
	 * @param node
	 * @param root
	 */
	private static void crawl(File node, File root)
	{
		System.out.println(root.getAbsolutePath());
		String path = node.getAbsolutePath().substring(root.getAbsolutePath().length() + ((node != root)?1:0));
		if (node != root)
			path += "/";
		
		System.out.println(path);
		for (File f : node.listFiles())
		{
			//ignore hidden files
			if (f.isHidden())
				continue;
			
			//ignore boogie file
			if (f.getName().equals("boogie.hash"))
				continue;
			
			//crawl directory trees
			if (f.isDirectory())
			{
				crawl(f, root);
			}
			else
			{
				System.out.println(path + "/" + f.getName());
				files.add(path + f.getName());
				paths.add(f.getAbsolutePath());
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
