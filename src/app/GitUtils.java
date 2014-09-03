package app;

import java.io.IOException;
import java.util.Arrays;

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ProgressMonitor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

/**
 * Just wrap some git functions to make them more convenient 
 * and standard to use with Boogie
 * @author nhydock
 *
 */
public class GitUtils {
	
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
	public static Thread cloneRepo(String repo, String branch, FileHandle downloadDir, ProgressMonitor monitor)
	{
		final CloneCommand cmd = Git.cloneRepository()
				.setURI(repo)
				.setBranchesToClone(Arrays.asList("refs/heads/"+branch))
				.setBranch(branch)
				.setDirectory(downloadDir.file());
		if (monitor != null)
			cmd.setProgressMonitor(monitor);
		
		Thread callThread = new Thread(new Runnable(){
			public void run(){
				try {
					cmd.call().close();
				} catch (GitAPIException e) {
					e.printStackTrace();
				}
			}
		});
		return callThread;
	}

	/**
	 * Checks if the git directory for the game exists already
	 * @return
	 */
	public static boolean gitExists()
	{
		FileHandle gitDir = internalToAbsolute(Boogie.GAME_DIR + "/.git");
		return gitDir.exists();
	}
	
	public static FileHandle internalToAbsolute(String file)
	{
		return Gdx.files.absolute(Gdx.files.internal(file).file().getAbsolutePath());
	}
	
	/**
	 * Pulls the latest git information from the specified git download directory
	 * @param gitDir
	 *   Directory where git data for the game is saved
	 * @param branch
	 *   Specific branch that we should be on and pulling from
	 * @param monitor
	 *   JGit progress monitor that can be hooked onto the pull cmd for reponsive feedback
	 * @return
	 *   A generated thread within which the command sit, used to prevent the app from hard stalling while git is downloading
	 * @throws IOException when Git directory can not be found or opened
	 */
	public static Thread pullRepo(final FileHandle gitDir, String branch, final ProgressMonitor monitor) throws IOException
	{
		final Git source = Git.open(gitDir.file());
		final CheckoutCommand checkout = source.checkout().setName(branch);
		final PullCommand pull = source.pull();
		pull.setRemote("origin");
		pull.setRemoteBranchName(branch);
		if (monitor != null)
			pull.setProgressMonitor(monitor);

		Thread callThread = new Thread(new Runnable(){
			public void run(){
				try {
					//first, insure the directory is on the right branch
					checkout.call();
					//then perform a pull on that branches data
					PullResult result = pull.call();
					MergeResult merge = result.getMergeResult();
					Gdx.app.log("Git", ""+merge.getMergeStatus());
					//close the git repository resources when we're done with it
					source.close();
				} catch (GitAPIException e) {
					e.printStackTrace();
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
