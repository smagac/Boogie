package app;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Scanner;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Just wrap some git functions to make them more convenient 
 * and standard to use with Boogie
 * @author nhydock
 *
 */
public class DownloadUtils {
	
	/**
	 * Prepares a clone repository command
	 * @param repo
	 *   URL of the git repository
	 * @param branch
	 *   Specific branch holding the builds of the game
	 * @param downloadDir
	 *   Directory to download the game into
	 * @param monitor
	 *   Progress monitor useful for hooking into the clone cmd and provide useful visual feedback
	 * @return a contained thread for executing and waiting on the command
	 */
	public static Thread cloneRepo(final String url, final FileHandle downloadDir)
	{
		try {
			final URL website = new URL(url+"boogie.hash");
			
			Thread callThread = new Thread(new Runnable(){
				public void run(){
					try (ReadableByteChannel rbc = Channels.newChannel(website.openStream());
						 FileOutputStream fos = new FileOutputStream(downloadDir.child("boogie.hash").file()) ) {
						fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
						//parse the hash file
						Thread pull = pullRepo(url, downloadDir, true);
						pull.start();
						while (pull.isAlive());
					} catch (IOException e) {
						e.printStackTrace();
						Gdx.app.exit();
					}
				}
			});
			return callThread;
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
			System.exit(-1);
			return null;
		}
	}

	/**
	 * Checks if the game directory for the game exists already
	 * @return
	 */
	public static boolean gameExists()
	{
		FileHandle gitDir = internalToAbsolute(Boogie.GAME_DIR + "/boogie.hash");
		return gitDir.exists();
	}
	
	public static FileHandle internalToAbsolute(String file)
	{
		return Gdx.files.absolute(Gdx.files.internal(file).file().getAbsolutePath());
	}
	
	/**
	 * Pulls the latest game information from the specified download directory
	 * @param gitDir
	 *   Directory where game data is saved
	 * @return
	 *   A generated thread within which the command sit, used to prevent the app from hard stalling while git is downloading
	 * @throws IOException when Game directory can not be found or opened
	 */
	public static Thread pullRepo(final String url, final FileHandle gitDir, final boolean force) throws IOException
	{
		final URL website = new URL(url+"boogie.hash");
		String hashReader = gitDir.child("boogie.hash").readString();
		final ObjectMap<FileHandle, URL> localToRemote = new ObjectMap<FileHandle, URL>();
		final ObjectMap<FileHandle, String> localHashes = new ObjectMap<FileHandle, String>();
		final ObjectMap<URL, String> remoteHashes = new ObjectMap<URL, String>();
		
		try (Scanner reader = new Scanner(hashReader))
		{
			while (reader.hasNextLine())
			{
				String line = reader.nextLine();
				String[] params = line.split(" ");
				
				localHashes.put(gitDir.child(params[1]), params[0]);
			}
		}
		
		try (ReadableByteChannel rbc = Channels.newChannel(website.openStream());
			 Scanner reader = new Scanner(rbc))
		{
			while (reader.hasNextLine())
			{
				String line = reader.nextLine();
				String[] params = line.split(" ");
				
				FileHandle local = gitDir.child(params[1]);
				URL remote = new URL(url + params[1]);
				remoteHashes.put(remote, params[0]);
				localToRemote.put(local, remote);
			}
		}

		Thread callThread = new Thread(new Runnable(){
			public void run(){
				for (FileHandle file : localToRemote.keys())
				{
					URL remote = localToRemote.get(file);
					String localHash = localHashes.get(file);
					String remoteHash = remoteHashes.get(remote);
					
					//fetch file from server if hash is not equal
					if (!localHash.equals(remoteHash) || force)
					{
						try (ReadableByteChannel rbc = Channels.newChannel(remote.openStream());
							 FileOutputStream fos = new FileOutputStream(file.file()) ) {
							fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		});
		
		return callThread;
	}

	/**
	 * Deletes the entire directory containing game files, in order to allow it to be redownloaded
	 * @return thread that executes deletion
	 */
	public static Thread clean() {
		final FileHandle gitDir = internalToAbsolute(Boogie.GAME_DIR);
		Thread deleteCmd = new Thread(new Runnable(){
			public void run(){
				gitDir.deleteDirectory();
			}
		});
				
		return deleteCmd;
	}
}
