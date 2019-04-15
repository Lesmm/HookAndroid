package common.modules.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.util.Log;

public class HLog {

	public static boolean debug = true;
	public static String appPackageName = null;
	public static String storeLogsDirecotry = null;

	private static final String TAG = "HLog";

	/*
	 *  fatal method
	 */
	public static synchronized void fatal(String string) {
		if (string == null) {
			string = "<NULL>";
		}

		d(string);

		writeToFile(string, true);
	}

	/*
	 *  log methods
	 */
	public static synchronized void log(Throwable e) {
		synchronized (HLog.class) {
			log("Exception -------------------->>>>>>");
			log(e.toString());
			log(e.getStackTrace());
			log("Exception <<<<<<--------------------");
		}
	}

	public static synchronized void log(StackTraceElement[] stack) {
		synchronized (HLog.class) {
			for (int i = 0; i < stack.length; i++) {
				StackTraceElement s = stack[i];
				log("	" + s.toString() /* + "[" + s.getFileName() + ":" + s.getLineNumber() + "]" */ );
			}
		}
	}

	public static synchronized void log(String string) {
		log(TAG, string);
	}

	public static synchronized void log(String tag, String string) {
		if (string == null) {
			string = "<NULL>";
		}

		d(tag, string);

		writeToFile(string, false);
	}

	/*
	 * d methods
	 */
	public static synchronized void d(Throwable e) {
		synchronized (HLog.class) {
			d("Exception -------------------->>>>>>");
			d(e.toString());
			log(e.getStackTrace());
			d("Exception <<<<<<--------------------");
		}
	}

	public static synchronized void d(StackTraceElement[] stack) {
		synchronized (HLog.class) {
			for (int i = 0; i < stack.length; i++) {
				StackTraceElement s = stack[i];
				d("	" + s.toString() /* + "[" + s.getFileName() + ":" + s.getLineNumber() + "]" */ );
			}
		}
	}

	public static synchronized void d(String string) {
		d(TAG, string);
	}

	public static synchronized void d(String tag, String string) {
		if (debug == false) {
			return;
		}

		Log.d(tag, string);
	}

	/*
	 * Util methods
	 */
	public static String exceptionToString(Exception e) {
		String message = e.toString();
		StackTraceElement[] stack = e.getStackTrace();
		for (int i = 0; i < stack.length; i++) {
			StackTraceElement s = stack[i];
			message += "\r\n" + "	" + s.toString();
		}
		return message;
	}

	/*
	 * Private methods
	 */
	private static void writeToFile(String message, boolean isFatal) {
		if (storeLogsDirecotry == null) {
			return;
		}
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
		String dayString = dateFormat.format(new Date());
		String fileName = dayString;
		if (appPackageName != null) {
			fileName = dayString + "_" + appPackageName;
		}
		if (isFatal) {
			fileName = fileName + "_" + "fatal";
		}
		fileName = fileName + ".log";
		String filePath = storeLogsDirecotry + "/" + fileName;

		writeToFile(message, filePath);
	}

	private static void writeToFile(String message, String filePath) {
		try {
			File file = new File(filePath);
			if (!file.exists()) {
				File parent = file.getParentFile();
				if (!parent.exists()) {
					parent.mkdirs();
					IFileUtil.chmod777(parent.getAbsolutePath());
				}

				Calendar rightNow = Calendar.getInstance();
				rightNow.setTime(new Date());
				rightNow.add(Calendar.DAY_OF_MONTH, -10);
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
				final String beforeDayString = dateFormat.format(rightNow.getTime());

				// delete the old log files
				File[] shouldDeleteFile = parent.listFiles(new FileFilter() {
					@Override
					public boolean accept(File pathname) {
						String fileName = pathname.getName();
						String a = fileName.replaceAll("[^0-9]", "");
						String b = beforeDayString.replaceAll("[^0-9]", "");
						int status = a.compareTo(b);
						boolean isBefore = status < 0;
						return isBefore;
					}
				});

				for (File subFile : shouldDeleteFile) {
					subFile.delete();
				}

				// create the new log file
				file.createNewFile();
				IFileUtil.chmod777(filePath);

			} else {
				if (file.length() >= 50 * 1024 * 1024) { // 50 MB
					splitFile(file);
				}
			}

			String time = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.US).format(new Date());
			String content = time + "  " + message;
			FileWriter writer = new FileWriter(file, true);
			writer.write(content);
			writer.flush();
			writer.close();
		} catch (IOException e) {
			Log.d(TAG, e.toString());
		}
	}

	private static void splitFile(File file) {
		try {
			long fileSize = file.length();
			String tempFileName = file.getAbsolutePath() + ".half";

			RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
			randomAccessFile.seek(fileSize / 2);

			FileWriter writer = new FileWriter(tempFileName);

			byte[] buffer = new byte[1024 * 1024];
			int readLength = 0;
			while ((readLength = randomAccessFile.read(buffer)) > 0) {
				String content = new String(buffer, 0, readLength);
				writer.write(content);
			}
			writer.flush();
			writer.close();

			randomAccessFile.close();

			file.delete();
			new File(tempFileName).renameTo(file);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
