package common.modules.util;

import android.os.Handler;
import android.os.Looper;

public class IHandlerUtil {

	public static void postToMainThread(Runnable runnable) {
		new Handler(Looper.getMainLooper()).post(runnable);
	}

	public static void postToMainThreadDelayed(double delaySecond, Runnable runnable) {
		new Handler(Looper.getMainLooper()).postDelayed(runnable, (long) (delaySecond * 1000));
	}
	
}
