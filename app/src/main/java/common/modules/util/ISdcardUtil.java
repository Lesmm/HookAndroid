package common.modules.util;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.os.Environment;

@SuppressLint("SdCardPath")
public class ISdcardUtil {

	public static final String SDCardPath = "/sdcard/";
	public static final String ExternalStoragePath = Environment.getExternalStorageDirectory().getAbsolutePath();

	public static boolean isInSDCard(String path) {
		String slashedPath = __fixSlashes__(path);
		return path.contains(ExternalStoragePath) || path.startsWith(SDCardPath) || slashedPath.contains(ExternalStoragePath)
				|| slashedPath.startsWith(SDCardPath);
	}

	public static void storeWriteSDCardLog(String logFileName, String slashedPath) {
		if (!new File(logFileName).exists()) {
			IFileUtil.writeTextToFile("", logFileName);
			IFileUtil.chmod777(logFileName);
		}
		
		String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
		String content = time + ": " + slashedPath + "\r\n";
		IFileUtil.appendTextToFile(content, logFileName);

		// check size
		File logFile = new File(logFileName);
		if (logFile.length() >= 10 * 1024 * 1024) { // 10MB
			IFileUtil.splitFile(logFile);
		}
	}

	public static void storePath(String jsonFileName, String slashedPath) {
		try {

			if (!new File(jsonFileName).exists()) {
				IFileUtil.writeTextToFile("{}", jsonFileName);
				IFileUtil.chmod777(jsonFileName);
			}

			String jsonString = IFileUtil.readFileToText(jsonFileName);
			JSONObject jsonObject = new JSONObject(jsonString);

			Iterator<?> it = jsonObject.keys();
			boolean isContained = false;
			while (it.hasNext()) {
				String key = (String) it.next();
				if (key.contains(slashedPath)) {
					isContained = true;
					break;
				}
				if (slashedPath.contains(key)) {
					jsonObject.remove(key);
					break;
				}
			}
			if (!isContained) {
				jsonObject.put(slashedPath, "");
			}
			IFileUtil.writeTextToFile(jsonObject.toString(), jsonFileName);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void deletePaths(String jsonFileName) {
		try {
			String jsonString = IFileUtil.readFileToText(jsonFileName);
			JSONObject jsonObject = new JSONObject(jsonString);

			Iterator<?> it = jsonObject.keys();
			while (it.hasNext()) {
				String slashedPath = (String) it.next();
				boolean retVal = new File(slashedPath).delete();
				HLog.log("Delete SDCard file: " + slashedPath + " -> " + retVal);
				
				// same path
				String samePath = null;
				if (slashedPath.contains(ExternalStoragePath)) {
					samePath = slashedPath.replace(ExternalStoragePath, SDCardPath);
				} else if (slashedPath.contains(SDCardPath)) {
					samePath = slashedPath.replace(SDCardPath, ExternalStoragePath);
				}
				if (samePath != null) {
					boolean val = new File(samePath).delete();
					HLog.log("Delete SDCard same file: " + samePath + " -> " + val);
				}
				
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	// Copy from source java.io.File.java
	// Removes duplicate adjacent slashes and any trailing slash.
	public static final char separatorChar = System.getProperty("file.separator", "/").charAt(0);

	public static String __fixSlashes__(String path) {
		// Remove duplicate adjacent slashes.
		boolean lastWasSlash = false;
		char[] newPath = path.toCharArray();
		int length = newPath.length;
		int newLength = 0;
		for (int i = 0; i < length; ++i) {
			char ch = newPath[i];
			if (ch == '/') {
				if (!lastWasSlash) {
					newPath[newLength++] = separatorChar;
					lastWasSlash = true;
				}
			} else {
				newPath[newLength++] = ch;
				lastWasSlash = false;
			}
		}
		// Remove any trailing slash (unless this is the root of the file system).
		if (lastWasSlash && newLength > 1) {
			newLength--;
		}
		// Reuse the original string if possible.
		return (newLength != length) ? new String(newPath, 0, newLength) : path;
	}
}
