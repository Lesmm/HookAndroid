package common.modules.util.manager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

@SuppressWarnings("unchecked")
public class ActivityManagerUtil {

	/*
	 * Action Methods
	 */

	public static void killProcess(Context context, String packageName) {
		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

		PackageManager pm = context.getPackageManager();
		List<ApplicationInfo> packages = pm.getInstalledApplications(0); // get a list of installed apps.
		for (ApplicationInfo packageInfo : packages) {
			if ((packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1) {
				continue;
			}
			if (packageInfo.packageName.equals(packageName)) {
				am.killBackgroundProcesses(packageInfo.packageName);
			}

		}
	}

	public static void forceStopPackage(Context context, String packageName) {
		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

		try {
			Method method = am.getClass().getMethod("forceStopPackage", String.class);
			method.setAccessible(true);
			method.invoke(am, packageName);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void moveTaskToFront(Context context, int taskId) {
		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		am.moveTaskToFront(taskId, 0);
	}

	/*
	 * Get Methods
	 */
	public static int getTaskIdForPackageName(Context context, String packageName) {
		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

		List<RunningTaskInfo> allTasks = am.getRunningTasks(100);
		if (allTasks != null && allTasks.size() > 0) {
			for (int i = 0; i < allTasks.size(); i++) {
				RunningTaskInfo taskInfo = allTasks.get(i);
				if (taskInfo.baseActivity.getPackageName().equals(packageName)) {
					return taskInfo.id;
				}
			}
		}

		return 0;
	}

	// getTopActivity().getTaskId())
	public static Activity getTopActivity() {
		try {
			Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
			Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
			Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
			activitiesField.setAccessible(true);
			Map<Object, Object> activities = (Map<Object, Object>) activitiesField.get(activityThread); // ArrayMap<IBinder, ActivityClientRecord>
			if (activities == null) {
				return null;
			}

			for (Object activityRecord : activities.values()) {
				Class<?> activityRecordClass = activityRecord.getClass();
				Field pausedField = activityRecordClass.getDeclaredField("paused");
				pausedField.setAccessible(true);
				if (!pausedField.getBoolean(activityRecord)) {
					Field activityField = activityRecordClass.getDeclaredField("activity");
					activityField.setAccessible(true);
					Activity activity = (Activity) activityField.get(activityRecord);
					return activity;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/*
	 * Is Methods
	 */
	public static boolean isAppRunning(Context context, String packageName) {
		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

		List<RunningTaskInfo> tasklist = am.getRunningTasks(100);
		for (RunningTaskInfo info : tasklist) {
			if (info.topActivity != null) {
				if (info.topActivity.getPackageName().equals(packageName)) {
					return true;
				}
			}
			if (info.baseActivity != null) {
				if (info.baseActivity.getPackageName().equals(packageName)) {
					return true;
				}
			}
		}
		return false;
	}

}
