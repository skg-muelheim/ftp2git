package inc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.TextProgressMonitor;

public class Main {
	public static void main(String[] args) throws IOException,
			InvalidRemoteException, TransportException, GitAPIException {
		
		String ftpServer = args[0];
		String ftpUser = args[1];
		String ftpPass = args[2];

		String gitUri = "https://github.com/skg-muelheim/sportkegler-muelheim.de.git";
		String gitBranch = "website-upstream";
		
		
		ProgressMonitor monitor = new TextProgressMonitor();
//		monitor = NullProgressMonitor.INSTANCE;

		long num = System.currentTimeMillis();
		num = 0;

		File tDir = new File("./tmp/" + num);

		Git git;
		if (tDir.exists()) {
			git = Git.open(tDir);
		} else {
			;
			git = Git
					.cloneRepository()
					.setProgressMonitor(monitor)
					//
					.setBranch(gitBranch)
					//
					 .setURI(gitUri)//
//					.setURI("C:/Users/Sebastian/deve/vagrant-boxes/skg-vagrant-lamp/.git/modules/public/sportkegler-muelheim.de")//
					.setDirectory(tDir)//
					.call()//
			;
		}

		FTPClient ftpClient = new FTPClient();
		ftpClient.addProtocolCommandListener(new PrintCommandListener(
				System.out));
		ftpClient.connect(ftpServer);
		boolean login = ftpClient.login(
				ftpUser, ftpPass);

		Map<String, Map<String, FTPFile>> files = new HashMap<>();
		recurseSearch(ftpClient, "www", files);
		recurseSearch(ftpClient, "cdn", files);

		deleteOld(tDir, files);
		update(tDir, ftpClient, files);

		ftpClient.logout();

	}

	private static void update(File tDir, FTPClient ftpClient,
			Map<String, Map<String, FTPFile>> files) throws IOException,
			FileNotFoundException {
		for (Entry<String, Map<String, FTPFile>> entry : files.entrySet()) {
			String path = entry.getKey();
			for (FTPFile ftpFile : entry.getValue().values()) {
				if (ftpFile.isFile()) {
					File destFile = new File(tDir, path + "/"
							+ ftpFile.getName());
					String name = destFile.getName().toLowerCase();
					System.out.println(destFile);

					long timeInMillis = ftpFile.getTimestamp()
							.getTimeInMillis();
					if (!destFile.exists()
							|| destFile.lastModified() != timeInMillis) {
						System.out.println(destFile.lastModified());
						System.out.println(timeInMillis);
						System.out.println(destFile.length());
						System.out.println(ftpFile.getSize());

						if (name.endsWith(".html") || name.endsWith(".js") || name.endsWith(".css")|| name.endsWith(".htaccess")|| name.endsWith(".svg")) {
							ftpClient.setFileType(FTP.ASCII_FILE_TYPE);
						}else {
							ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
						}

						destFile.getParentFile().mkdirs();
						boolean done = false;
						while (!done) {
							FileOutputStream fout = new FileOutputStream(
									destFile);
							done = ftpClient.retrieveFile(
									path + "/" + ftpFile.getName(), fout);
							fout.close();
						}
						destFile.setLastModified(timeInMillis);
					}
				}
			}
		}
	}

	private static void deleteOld(File tDir,
			Map<String, Map<String, FTPFile>> files) {
		for (String path : files.keySet()) {
			System.out.println(path);
			Map<String, FTPFile> ftpInfo = files.get(path);
			File checkDir = new File(tDir, path);
			File[] listFiles = checkDir.listFiles();
			if (listFiles != null) {
				for (File file : listFiles) {
					if (!ftpInfo.containsKey(file.getName())) {
						file.delete();
					}
				}
			}
		}
	}

	private static void recurseSearch(FTPClient ftpClient, String path,
			Map<String, Map<String, FTPFile>> files) throws IOException {
		System.out.println("RECURSIVE " + path);
		FTPFile[] listDirectories = ftpClient.listDirectories(path);
		for (FTPFile ftpFile : listDirectories) {
			if (!ftpFile.getName().equals("..")) {
				recurseSearch(ftpClient, path + "/" + ftpFile.getName(), files);
			}
		}
		Map<String, FTPFile> pathContents = new HashMap<>(64);
		files.put(path, pathContents);

		System.out.println("LIST FILES " + path);
		FTPFile[] listFiles = ftpClient.listFiles(path);
		for (FTPFile ftpFile : listFiles) {
			pathContents.put(ftpFile.getName(), ftpFile);
			if (!ftpFile.isDirectory()) {
				System.out.println(ftpFile);
			}
		}
	}
}
