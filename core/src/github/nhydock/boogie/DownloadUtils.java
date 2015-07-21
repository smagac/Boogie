package github.nhydock.boogie;

import github.nhydock.boogie.view.StateMessage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Net.HttpResponse;
import com.badlogic.gdx.ai.msg.MessageManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Just wrap some downloading functions to make them more convenient and standard to use
 * with Boogie
 * 
 * @author nhydock
 *
 */
public class DownloadUtils
{
	static final ObjectMap<String, FileHandle> tempFiles = new ObjectMap<String, FileHandle>();
	static final Array<FileDownloader> downloaders = new Array<FileDownloader>();
	static final Array<String> toDelete = new Array<String>();

	private static class FileDownloader implements Net.HttpResponseListener
	{

		String filename;
		FileHandle temp;
		File out;

		Runnable success;

		public FileDownloader(String filename)
		{
			this.filename = filename;
			temp = internalToAbsolute(".tmp/" + filename);
			out = temp.file();
		}

		public FileDownloader(String filename, Runnable after)
		{
			this(filename);
			success = after;
		}

		@Override
		public void handleHttpResponse(HttpResponse httpResponse)
		{
			downloaders.add(this);

			internalToAbsolute(".tmp/").file().mkdir();
			temp.parent().mkdirs();
			try (ReadableByteChannel rbc = Channels.newChannel(httpResponse.getResultAsStream());
					FileOutputStream fos = new FileOutputStream(out))
			{
				fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
				tempFiles.put(filename, temp);
				Gdx.app.log("Download", "Finished downloading " + filename);
			} catch (IOException e)
			{
				e.printStackTrace();
				out.delete();
			}

			downloaders.removeValue(this, false);

			success.run();
		}

		@Override
		public void failed(Throwable t)
		{
			MessageManager.getInstance().dispatchMessage(null, StateMessage.DownloadFailed);
			out.delete();
		}

		@Override
		public void cancelled()
		{
			MessageManager.getInstance().dispatchMessage(null, StateMessage.DownloadCancelled);
			out.delete();
		}

		public boolean equals(Object o)
		{
			if (o == null)
				return false;
			if (o == this)
				return true;
			if (o instanceof FileDownloader)
			{
				FileDownloader f = (FileDownloader) o;
				if (f.filename.equals(this.filename))
				{
					return true;
				}
			}
			return false;
		}
	}

	private static class BoogieHashReader implements Runnable
	{
		String url;

		public BoogieHashReader(String url)
		{
			this.url = url;
		}

		public void run()
		{
			Array<String> files = new Array<String>();
			files.add("boogie.hash");
			try (Scanner scanner = new Scanner(new FileInputStream(new File(".tmp/boogie.hash"))))
			{
				while (scanner.hasNext())
				{
					scanner.next();
					files.add(scanner.next());
				}

			} catch (IOException e)
			{
				e.printStackTrace();
			}

			Runnable after = new CheckDownloads(files.size);
			for (String file : files)
			{

				Net.HttpRequest get = new Net.HttpRequest(Net.HttpMethods.GET);
				get.setUrl(url + file);
				get.setTimeOut(5000);

				Gdx.net.sendHttpRequest(get, new FileDownloader(file, after));
			}

		}

	}

	private static class CheckDownloads implements Runnable
	{

		int size;

		public CheckDownloads(int size)
		{
			this.size = size;
		}

		@Override
		public void run()
		{
			if (tempFiles.size >= size)
			{
				MessageManager.getInstance().dispatchMessage(null, StateMessage.DownloadCompleted);
			}
		}

	}

	/**
	 * Prepares a clone repository command
	 * 
	 * @param repo
	 *            URL of the download repository
	 * @param branch
	 *            Specific branch holding the builds of the game
	 * @param downloadDir
	 *            Directory to download the game into
	 * @param monitor
	 *            Progress monitor useful for hooking into the clone cmd and
	 *            provide useful visual feedback
	 * @return a contained thread for executing and waiting on the command
	 */
	public static void cloneRepo(final String url, final FileHandle downloadDir)
	{
		Gdx.app.log("Download", "Fetching boogie.hash from " + url);
		Net.HttpRequest get = new Net.HttpRequest(Net.HttpMethods.GET);
		get.setUrl(url + "boogie.hash");
		get.setTimeOut(5000);
		Gdx.net.sendHttpRequest(get, new FileDownloader("boogie.hash", new BoogieHashReader(url)));
	}

	/**
	 * Checks if the game directory for the game exists already
	 * 
	 * @return
	 */
	public static boolean gameExists()
	{
		FileHandle gameDir = internalToAbsolute(Boogie.GAME_DIR + "/boogie.hash");
		return gameDir.exists();
	}

	public static FileHandle internalToAbsolute(String file)
	{
		return Gdx.files.absolute(Gdx.files.internal(file).file().getAbsolutePath());
	}

	/**
	 * Pulls the latest game information from the specified download directory
	 * 
	 * @param gameDir
	 *            Directory where game data is saved
	 */
	public static void pullRepo(final String url, final FileHandle gameDir, final boolean force)
	{
		final Net.HttpRequest get = new Net.HttpRequest(Net.HttpMethods.GET);
		get.setUrl(url + "boogie.hash");
		get.setTimeOut(5000);
		Gdx.net.sendHttpRequest(get, new FileDownloader("boogie.hash", new Runnable(){

			@Override
			public void run()
			{
				final Array<String> remoteFiles = new Array<String>();
				final ObjectMap<String, String> localHashes = new ObjectMap<String, String>();
				final ObjectMap<String, String> remoteHashes = new ObjectMap<String, String>();

				try (Scanner reader = new Scanner(internalToAbsolute(".tmp/boogie.hash").read()))
				{
					while (reader.hasNextLine())
					{
						String line = reader.nextLine();
						String[] params = line.split(" ");

						remoteHashes.put(params[1], params[0]);
						remoteFiles.add(params[1]);
					}
				}

				try (Scanner reader = new Scanner(gameDir.child("boogie.hash").read()))
				{
					while (reader.hasNextLine())
					{
						String line = reader.nextLine();
						String[] params = line.split(" ");
						if (remoteFiles.contains(params[1], false))
						{
							localHashes.put(params[1], params[0]);
						} else
						{
							toDelete.add(params[1]);
						}
					}
				}

				Array<String> download = new Array<String>();
				for (String file : remoteFiles)
				{
					String localHash = localHashes.get(file);
					String remoteHash = remoteHashes.get(file);
					FileHandle f = internalToAbsolute(Boogie.GAME_DIR + file);

					// just download the file if the local one doesn't exist
					if (localHash == null || !f.exists() || !localHash.equals(remoteHash) || force)
					{
						download.add(file);
					}
				}

				if (download.size > 0) {
					Runnable after = new CheckDownloads(download.size+1);
					for (String file : download)
					{
						Net.HttpRequest get = new Net.HttpRequest(Net.HttpMethods.GET);
						get.setUrl(url + file);
						get.setTimeOut(5000);
	
						Gdx.net.sendHttpRequest(get, new FileDownloader(file, after));
					}
				} else {
					MessageManager.getInstance().dispatchMessage(null, StateMessage.DownloadCompleted);
				}
			}
			
		}));
		
		

	}

	/**
	 * Copies over the temp files to the game's directory
	 */
	public static void writePerm()
	{
		FileHandle gameDir = internalToAbsolute(Boogie.GAME_DIR);
		gameDir.mkdirs();

		// delete excess game files
		for (String file : toDelete)
		{
			FileHandle out = internalToAbsolute(Boogie.GAME_DIR + file);
			out.delete();
		}

		for (String file : tempFiles.keys())
		{
			FileHandle tmp = tempFiles.get(file);
			FileHandle out = internalToAbsolute(Boogie.GAME_DIR + file);
			out.parent().mkdirs();
			Gdx.app.log("Move", "moving to " + out.path());
			try
			{
				java.nio.file.Files.move(tmp.file().toPath(), out.file().toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}

		tempFiles.clear();
		toDelete.clear();
		Gdx.files.internal(".tmp").deleteDirectory();
	}

	/**
	 * Deletes the entire directory containing game files, in order to allow it
	 * to be redownloaded
	 * 
	 * @return thread that executes deletion
	 */
	public static void clean()
	{
		final FileHandle gameDir = internalToAbsolute(Boogie.GAME_DIR);
		gameDir.deleteDirectory();
	}
}
