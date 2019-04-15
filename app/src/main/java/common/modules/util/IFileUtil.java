package common.modules.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;

public class IFileUtil {
	
	public static final int S_IRWXU = 00700;
	public static final int S_IRUSR = 00400;
	public static final int S_IWUSR = 00200;
	public static final int S_IXUSR = 00100;

	public static final int S_IRWXG = 00070;
	public static final int S_IRGRP = 00040;
	public static final int S_IWGRP = 00020;
	public static final int S_IXGRP = 00010;

	public static final int S_IRWXO = 00007;
	public static final int S_IROTH = 00004;
	public static final int S_IWOTH = 00002;
	public static final int S_IXOTH = 00001;
	
	public static final int SPERMS777 = S_IRUSR | S_IWUSR | S_IXUSR | S_IRGRP | S_IWGRP | S_IXGRP | S_IROTH | S_IWOTH | S_IXOTH;

	public static void chmod777(String fileName) {
		chmod(fileName, SPERMS777, -1, -1);
	}
	
	public static void chmod(String fileName, int perms, int uid, int gid) {
		try {
			Class<?> clsFileUtils = String.class.getClassLoader().loadClass("android.os.FileUtils");
			Method md = clsFileUtils.getDeclaredMethod("setPermissions", new Class[] { String.class, int.class, int.class, int.class });
			md.setAccessible(true);
			md.invoke(clsFileUtils, new Object[] { fileName, perms, uid, gid });
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String readFileToText(String fileName) {
		String result = null;
		try {
			FileInputStream fileInputStream = new FileInputStream(fileName);
			DataInputStream dataInputStream = new DataInputStream(fileInputStream);

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			int length = -1;
			// do not use new File(fileName).length to init a buffer. /proc/PID/status file length is 0, but it has contents
			byte[] buffer = new byte[1024 * 100];
			while(( length = dataInputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, length);
			}

			dataInputStream.close();
			byte[] bytes = outputStream.toByteArray();
			// String contents = outputStream.toString();
			result = new String(bytes, "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	public static boolean writeTextToFile(String text, String fileName) {
		boolean result = false;
		FileWriter fileWriter = null;
		try {
			File fileText = new File(fileName);
			File fileParent = fileText.getParentFile();
			if (!fileParent.exists()) {
				fileParent.mkdirs();
			}
			if (!fileText.exists()) {
				fileText.createNewFile();
			}
			fileWriter = new FileWriter(fileText, false);
			fileWriter.write(text);
			fileWriter.flush();
			fileWriter.close();
			result = true;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (fileWriter != null) {
					fileWriter.close();
				}
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		return result;
	}

	public static boolean appendTextToFile(String strBuffer, String strFilename) {
		FileWriter fileWriter = null;
		try {
			File fileText = new File(strFilename);
			File fileParent = fileText.getParentFile();
			if (!fileParent.exists()) {
				fileParent.mkdirs();
			}
			if (!fileText.exists()) {
				fileText.createNewFile();
			}
			fileWriter = new FileWriter(fileText, true);
			fileWriter.write(strBuffer);
			fileWriter.flush();
			fileWriter.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (fileWriter != null) {
					fileWriter.close();
				}
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		return false;
	}

	public static void devideFile(File f) {
		BufferedInputStream bufferedInputStream = null;
		try {
			int sizeOfFiles = (int) (f.length() / 1.8);
			byte[] buffer = new byte[sizeOfFiles];
			String fileName = f.getName();

			// try-with-resources to ensure closing stream
			int i = 0;
			bufferedInputStream = new BufferedInputStream(new FileInputStream(f));

			int bytesAmount = 0;
			while ((bytesAmount = bufferedInputStream.read(buffer)) > 0) {
				i++;
				// write each chunk of data into separate file with different number in name
				if (i == 2) {
					File newFile = new File(f.getParent(), fileName);
					FileOutputStream out = new FileOutputStream(newFile);
					out.write(buffer, 0, bytesAmount);
					out.flush();
					out.close();
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (bufferedInputStream != null) {
				try {
					bufferedInputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	public static void splitFile(File f) {
		FileInputStream fis = null;
		BufferedInputStream bis = null;
		try {
			int sizeOfFiles = (int) (f.length() / 1.8);
			byte[] buffer = new byte[sizeOfFiles];

			String fileName = f.getName();

			// try-with-resources to ensure closing stream
			int i = 0;
			fis = new FileInputStream(f);
			bis = new BufferedInputStream(fis);

			int bytesAmount = 0;
			while ((bytesAmount = bis.read(buffer)) > 0) {
				i++;
				// write each chunk of data into separate file with different number in name
				if (i == 2) {
					String filePartName = fileName;
					File newFile = new File(f.getParent(), filePartName);
					FileOutputStream out = null;
					try {
						out = new FileOutputStream(newFile);
						out.write(buffer, 0, bytesAmount);
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						try {
							if (out != null)
								out.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (bis != null)
					bis.close();
				if (fis != null)
					fis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
