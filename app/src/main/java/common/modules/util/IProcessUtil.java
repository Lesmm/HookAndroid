package common.modules.util;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IProcessUtil {

	/*
	 * java.lang.Process
	 */

	// mount, then get the info of /system, then search the DIR in AOSP
	// change ro -> rw, in android/device/xiaomi/cancro/rootdir/root/ fastab.qcom and init.qcom.rc
	// "mount -o rw,remount /system";

	public static String execCommandsWithSu(String command, String... subCommands) {

		java.lang.Process process = processCommandsWithSu(command, subCommands);
		int pid = getPidFromJavaProcess(process);
		String result = getJavaProcessExecuteResult(process);
		if (process != null) {
			process.destroy();
			HLog.d("Execute su command done: " + command + " for pid: " + pid);
		}
		return result;

	}

	public static Process processCommandsWithSu(String command, String... subCommands) {
		HLog.d("Process su command: " + command);
		int pid = -1;
		java.lang.Process process = null;

		try {

			process = Runtime.getRuntime().exec("su"); // ProcessManager.getInstance().exec(...);
			pid = getPidFromJavaProcess(process);
			HLog.d("Process su command: " + command + " in pid: " + pid);

			OutputStream outputStream = process.getOutputStream();
			DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
			dataOutputStream.writeBytes(command + "\n");

			for (int i = 0; i < subCommands.length; i++) {
				String subCommand = subCommands[i];
				HLog.d("Process su subcommand: " + subCommand);
				dataOutputStream.writeBytes(subCommand + "\n");
			}

			dataOutputStream.writeBytes("exit\n");
			dataOutputStream.flush();
			dataOutputStream.close();

		} catch (Exception e) {
			e.printStackTrace();

			if (process != null) {
				process.destroy();
				process = null;
				HLog.d("Process su command exception: " + command + " for pid: " + pid);
			}

		}

		return process;
	}

	public static String execCommands(String command, String... subCommands) {

		java.lang.Process process = processCommands(command, subCommands);
		int pid = getPidFromJavaProcess(process);
		String result = getJavaProcessExecuteResult(process);
		if (process != null) {
			process.destroy();
			HLog.d("Execute command done: " + command + " for pid: " + pid);
		}
		return result;

	}

	public static Process processCommands(String command, String... subCommands) {
		HLog.d("Process command: " + command);
		int pid = -1;
		java.lang.Process process = null;
		try {
			process = Runtime.getRuntime().exec(command);
			pid = getPidFromJavaProcess(process);
			HLog.d("Process command: " + command + " in pid: " + pid);

			OutputStream outputStream = process.getOutputStream();
			DataOutputStream dataOutputStream = new DataOutputStream(outputStream);

			for (int i = 0; i < subCommands.length; i++) {
				String subCommand = subCommands[i];
				HLog.d("Process subcommand: " + subCommand);
				dataOutputStream.writeBytes(subCommand + "\n");
			}

			dataOutputStream.writeBytes("exit\n");
			dataOutputStream.flush();
			dataOutputStream.close();

		} catch (Exception e) {
			e.printStackTrace();

			if (process != null) {
				process.destroy();
				process = null;
				HLog.d("Process command exception: " + command + " for pid: " + pid);
			}

		}

		return process;
	}

	public static String executeCommandWithArgs(String... command) {
		String result = "";
		HLog.d("Execute Command With Args: " + command);
		int pid = -1;
		java.lang.Process process = null;
		try {
			process = new ProcessBuilder().command(command).start(); // ProcessManager.getInstance().exec(...);
			pid = getPidFromJavaProcess(process);
			HLog.d("Execute Command With Args: " + " for pid: " + pid);

			result = getJavaProcessExecuteResult(process);
		} catch (IOException e) {
			result = e.getMessage();
		} finally {
			try {
				if (process != null) {
					process.destroy();
					HLog.d("Execute Command With Args: " + command + " for pid: " + pid);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	public static String getJavaProcessExecuteResult(java.lang.Process process) {
		if (process == null) {
			return null;
		}
		String result = "";
		InputStream inIs = null;
		InputStream errIs = null;

		try {
			process.waitFor();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int read = -1;

			// read standard
			inIs = process.getInputStream();
			while (inIs.available() != 0 && (read = inIs.read()) != -1) {
				baos.write(read);
			}

			// read error
			errIs = process.getErrorStream();
			while (errIs.available() != 0 && (read = errIs.read()) != -1) {
				baos.write(read);
			}

			result = new String(baos.toByteArray());

		} catch (Exception e) {
			result = e.getMessage();

		} finally {
			// close
			try {
				if (inIs != null) {
					inIs.close();
					inIs = null;
				}
				if (errIs != null) {
					errIs.close();
					errIs = null;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return result;
	}

	// java.lang.ProcessManager.ProcessImpl, for Android OS implement
	public static int getPidFromJavaProcess(java.lang.Process process) {
		if (process == null) {
			return -1;
		}
		try {
			Field pidField = process.getClass().getDeclaredField("pid");
			pidField.setAccessible(true);
			return (Integer) pidField.get(process);
		} catch (Exception e) {
			e.printStackTrace();
		}

		Pattern pattern = Pattern.compile("pid=(\\d+)");
		Matcher matcher = pattern.matcher(process.toString());
		if (matcher.find()) {
			return Integer.parseInt(matcher.group(1));
		}

		return -1;
	}

	public static void killJavaProcess(java.lang.Process process) {
		if (process == null) {
			return;
		}
		try {
			int pid = getPidFromJavaProcess(process);

			process.destroy();

			Runtime.getRuntime().exec("kill -9 " + pid);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * android.os.Process
	 */

	public static boolean isSystemUser() {
		// 0 for root; 1000 for system_server
		int uid = android.os.Process.myUid();
		if (uid == 0 || uid == 1000) {
			return true;
		}
		return false;
	}

	// android.os.Process.ProcessStartResult
	public static int getPidFromProcessStartViaZygoteResult(Object processStartResult) {
		try {
			Field pidField = processStartResult.getClass().getDeclaredField("pid");
			pidField.setAccessible(true);
			return (Integer) pidField.get(processStartResult);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	public static int getPidByUid(int uid) {
		int pid = -1;

		String uidValue = String.valueOf(uid);
		File[] processFiles = new File("/proc/").listFiles(new FileFilter() {
			@Override
			public boolean accept(File arg0) {
				return arg0.getName().matches("\\d*");
			}
		});

		for (int i = 0; i < processFiles.length; i++) {
			try {
				String processStatusPath = processFiles[i].getAbsolutePath() + "/status";
				String contents = IFileUtil.readFileToText(processStatusPath);
				if (contents != null) {
					Pattern pattern = Pattern.compile("Uid:(.*)");
					Matcher matcher = pattern.matcher(contents);

					if (matcher.find()) {
						String string = matcher.group(1);
						String[] strings = string.trim().split("\\s+");
						for (int j = 0; j < strings.length; j++) {
							String uidString = strings[j];
							if (uidValue.equals(uidString)) {
								String number = processStatusPath.replaceAll("[\\D]", "");
								pid = Integer.parseInt(number);
								break;
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		HLog.d("-----> GetPidByUid(" + uidValue + ") -> " + pid);
		return pid;
	}

	public static String[] getProcessInformations() {
		String result = IProcessUtil.executeCommandWithArgs("ps");
		String[] processes = result.split("\n");
		if (processes.length <= 1) {
			processes = result.split("\r\n");
		}
		if (processes.length <= 1) {
			processes = result.split("\r");
		}
		return processes;
	}

	public static Map<String, Integer> getProcessNamePidPairs() {
		String[] processes = getProcessInformations();

		int pidIndex = -1;

		// get pid index
		String headers = processes[0];
		String[] headerLabels = headers.trim().split("\\s+");
		for (int index = 0; index < headerLabels.length; index++) {
			String label = headerLabels[index];
			if (label.toLowerCase(Locale.US).equals("pid")) {
				pidIndex = index;
			}
		}

		// name pid pairs
		Map<String, Integer> results = new HashMap<String, Integer>();
		for (int i = 1; i < processes.length; i++) {
			String line = processes[i];
			String[] values = line.trim().split("\\s+");

			if (pidIndex != -1 && values.length > pidIndex) {
				String pidValue = values[pidIndex];
				String pidName = values[values.length - 1];

				int pidVal = Integer.parseInt(pidValue);
				results.put(pidName, pidVal);
			}
		}

		return results;
	}

	public static List<Integer> getPidsByName(String containsName) {
		ArrayList<Integer> results = new ArrayList<Integer>();
		Map<String, Integer> processNamePidPairs = getProcessNamePidPairs();

		for (Entry<String, Integer> entry : processNamePidPairs.entrySet()) {
			String name = entry.getKey();
			Integer pidInt = entry.getValue();
			if (name.contains(containsName)) {
				results.add(pidInt);
			}
		}

		return results;
	}

	public static boolean killProocessesByName(String containsName) {
		boolean result = false;
		List<Integer> pids = getPidsByName(containsName);
		for (Integer pid : pids) {
			result = result && killProcessByPid(pid);
		}
		return result;
	}

	public static boolean killProcessByUid(int uid) {
		int pid = getPidByUid(uid);
		if (pid != -1) {
			return killProcessByPid(pid);
		}
		return false;
	}

	public static boolean killProcessByPid(int pid) {
		HLog.d("-----> Killing process pid: " + pid);
		android.os.Process.killProcess(pid);

		// Check if exited
		boolean isPidExisted = false;

		// USER PID PPID VSIZE RSS WCHAN PC NAME
		int checkIfExistedCount = 10;
		while (checkIfExistedCount > 0) {

			isPidExisted = false;

			// check pid if exist using ps, further look into 'ps' source codes
			Map<String, Integer> pairs = getProcessNamePidPairs();
			for (Entry<String, Integer> entry : pairs.entrySet()) {
				int pidInt = entry.getValue();
				if (pidInt == pid) {
					isPidExisted = true;
				}
			}

			HLog.d("-----> Checking process pid: " + pid + " is existed: " + isPidExisted + " -> " + checkIfExistedCount);
			if (isPidExisted == false) {
				break;
			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			checkIfExistedCount--;
		}

		HLog.d("-----> KillProcessByPid(" + pid + ") -> " + (isPidExisted ? "failed" : "success"));
		return isPidExisted;
	}

	public static void setMyOomadj() {
		if (android.os.Process.myUid() == 0) {
			try {
				RandomAccessFile rf = new RandomAccessFile("/proc/" + android.os.Process.myPid() + "/oom_adj", "rw");
				rf.setLength(0);
				rf.write("-16".getBytes());
				rf.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}