package common.modules.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

public class IActivityUtil {

	/*
	 * Find View
	 */

	// Find Button\EditText ... The subclasses of TextView.
	public static TextView findTextViewByText(Activity activity, String text) {
		if (activity == null) return null;
		return findTextViewByText(activity.getWindow().getDecorView(), text);
	}

	public static TextView findTextViewByText(View view, String text) {
		if (view == null) {
			return null;
		}

		if (view instanceof TextView) {
			TextView textView = (TextView) view;
			if (textView.getText().toString().trim().equals(text)) {
				return textView;
			}
		}

		if (view instanceof ViewGroup) {
			ViewGroup viewGroup = (ViewGroup) view;
			for (int i = 0; i < viewGroup.getChildCount(); i++) {
				View child = viewGroup.getChildAt(i);
				TextView textView = findTextViewByText(child, text);
				if (textView != null) {
					return textView;
				}
			}
		}

		return null;
	}

	public static View findViewById(Activity activity, int id) {
		if (activity == null) return null;
		return findViewById(activity.getWindow().getDecorView(), id);
	}

	public static View findViewById(View view, int id) {
		if (view == null) {
			return null;
		}

		if (view.getId() == id) {
			return view;
		}

		if (view instanceof ViewGroup) {
			ViewGroup viewGroup = (ViewGroup) view;
			for (int i = 0; i < viewGroup.getChildCount(); i++) {
				View child = viewGroup.getChildAt(i);
				View result = findViewById(child, id);
				if (result != null) {
					return result;
				}
			}
		}

		return null;
	}

	public static LinkedList<View> findViewsById(Activity activity, int id) {
		LinkedList<View> list = new LinkedList<View>();
		if (activity != null) {
			findViewsById(activity.getWindow().getDecorView(), id, list);
		}
		return list;
	}

	public static void findViewsById(View view, int id, LinkedList<View> list) {
		if (view == null) return;
		if (view.getId() == id) {
			list.add(view);
		}

		if (view instanceof ViewGroup) {
			ViewGroup viewGroup = (ViewGroup) view;
			for (int i = 0; i < viewGroup.getChildCount(); i++) {
				View child = viewGroup.getChildAt(i);
				findViewsById(child, id, list);
			}
		}
	}

	/*
	 * x & y
	 */

	public static int getLeftFromScreen(View view) {
		if (view == null) return  -1;
		ViewParent vp = view.getParent();
		if (!(vp instanceof View)) {
			vp = null;
		}
		if (vp != null) {
			return getLeftFromScreen((View) vp) + view.getLeft() - view.getScrollX();
		}
		return view.getLeft() - view.getScrollX();
	}

	public static int getTopFromScreen(View view) {
		if (view == null) return  -1;
		ViewParent vp = view.getParent();
		if (!(vp instanceof View)) {
			vp = null;
		}
		if (vp != null) {
			return getTopFromScreen((View) vp) + view.getTop() - view.getScrollY();
		}
		return view.getTop() - view.getScrollY();
	}

	

	/*
	 * Is
	 */

	public static boolean isDialogActive(Dialog dialog) {
		if (dialog != null && dialog.isShowing()) {// && dlg.getWindow().isActive()) {
			return true;
		}
		return false;
	}

	public static boolean isActivityActive(Activity activity) {
		if (activity != null && !activity.isFinishing() && !activity.isRestricted() && activity.getWindow().isActive()
				&& !isActivityPausedOrStopped(activity)) {
			return true;
		}
		return false;
	}

	public static boolean isActivityPausedOrStopped(Activity activity) {
		try {
			Class<?> activityThreadClazz = String.class.getClassLoader().loadClass("android.app.ActivityThread");
			if (activityThreadClazz == null) {
				activity.getClass().getClassLoader().loadClass("android.app.ActivityThread");
			}
			Method currentActivityThreadMethod = activityThreadClazz.getDeclaredMethod("currentActivityThread", new Class[] {});
			currentActivityThreadMethod.setAccessible(true);
			Object currentActivityThread = currentActivityThreadMethod.invoke(activityThreadClazz, new Object[] {});

			if (currentActivityThread == null) {
				Field localValuesField = Thread.class.getDeclaredField("localValues");
				localValuesField.setAccessible(true);
				Object mainThreadLocalValues = localValuesField.get(Looper.getMainLooper().getThread());

				Field tableField = mainThreadLocalValues.getClass().getDeclaredField("table");
				tableField.setAccessible(true);
				Object[] table = (Object[]) tableField.get(mainThreadLocalValues);

				for (int i = 0; i < table.length; i++) {
					if (table[i] != null && table[i].getClass().getName().equals("android.app.ActivityThread")) {
						currentActivityThread = table[i];
						break;
					}
				}
			}

			if (currentActivityThread != null) {

				Field mActivitiesFields = activityThreadClazz.getDeclaredField("mActivities");
				mActivitiesFields.setAccessible(true);
				Object mActivities = mActivitiesFields.get(currentActivityThread);

				Method getMethod = mActivities.getClass().getDeclaredMethod("get", new Class[] { Object.class });
				getMethod.setAccessible(true);

				Field mTokenField = Activity.class.getDeclaredField("mToken");
				mTokenField.setAccessible(true);
				IBinder mToken = (IBinder) mTokenField.get(activity);

				// android.app.ActivityThread.ActivityClientRecord
				Object activityClientRecord = getMethod.invoke(mActivities, new Object[] { mToken });
				if (activityClientRecord != null) {
					Field fieldPaused = activityClientRecord.getClass().getDeclaredField("paused");
					fieldPaused.setAccessible(true);

					Field fieldStopped = activityClientRecord.getClass().getDeclaredField("stopped");
					fieldStopped.setAccessible(true);

					boolean paused = fieldPaused.getBoolean(activityClientRecord);
					boolean stopped = fieldPaused.getBoolean(activityClientRecord);

					if (paused) {
						return true;
					}
					if (stopped) {
						return true;
					}

				} else {
					return true;
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	/*
	 * Event 
	 */
	
	public static void sendKeyEvent(Dialog dialog, int key) {
		if (isDialogActive(dialog) == false)
			return;
		android.view.KeyEvent kv = new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, key);
		dialog.dispatchKeyEvent(kv);
		try {
			Thread.sleep(10);
		} catch (Exception e) {
			// nothing ...
		}
		kv = new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, key);
		dialog.dispatchKeyEvent(kv);
	}
	
	public static void sendKeyEvent(Activity activity, int key) {
		if (isActivityActive(activity) == false)
			return;
		android.view.KeyEvent kv = new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, key);
		activity.dispatchKeyEvent(kv);
		try {
			Thread.sleep(10);
		} catch (Exception e) {
			// nothing ...
		}
		kv = new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, key);
		activity.dispatchKeyEvent(kv);
	}

	public static void sendMouseClick(Activity activity, int x, int y) {
		if (isActivityActive(activity) == false) return;

		focusToActvityWindow(activity);

		MotionEvent eventDown = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, x, y, 0);
		/*boolean r1 =*/ activity.dispatchTouchEvent(eventDown);
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		MotionEvent eventUP = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, y, 0);
		/*boolean r2 =*/ activity.dispatchTouchEvent(eventUP);
	}
	
	public static void sendMouseClick(View view, int x, int y) {
		if (view == null) return;

		MotionEvent eventDown = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, x, y, 0);
		/*boolean r1 =*/ view.dispatchTouchEvent(eventDown);
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		MotionEvent eventUP = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, y, 0);
		/*boolean r2 =*/ view.dispatchTouchEvent(eventUP);
	}
	
	public static void sendMouseScrollEvent(Activity act, int x, int y1, int y2, long sltime) {
		if (isActivityActive(act) == false)
			return;

		focusToActvityWindow(act);

		MotionEvent eventDown = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, x, y1, 0);
		/*boolean r1 =*/ act.dispatchTouchEvent(eventDown);
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		MotionEvent eventUP = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, y2, 0);
		/*boolean r2 =*/ act.dispatchTouchEvent(eventUP);

	}
	
	public static void focusToActvityWindow(Activity activity) {
		try {
			
			IBinder binder = activity.getWindow().getDecorView().getWindowToken();
			
			Class<?> clazzIWindow = Class.forName("android.view.IWindow");
			Class<?>[] classesIWindow = clazzIWindow.getClasses();
			Class<?> firstClazz = classesIWindow[0];
			
			Method asInterfaceMethod = firstClazz.getMethod("asInterface", new Class[] { IBinder.class });
			Object ibinderProxyObj = (Object) asInterfaceMethod.invoke(firstClazz, new Object[] { binder });
			
			Method windowFocusChangedMethod = ibinderProxyObj.getClass().getMethod("windowFocusChanged", new Class[] { boolean.class, boolean.class });
			windowFocusChangedMethod.invoke(ibinderProxyObj, new Object[] { true, true });
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static boolean clickDialogButtonByText(AlertDialog dialog, String text) {
		int buttons[] = new int[] {DialogInterface.BUTTON_POSITIVE, DialogInterface.BUTTON_NEUTRAL, DialogInterface.BUTTON_NEGATIVE};
		for (int i = 0; i < buttons.length; i++) {
			Button button = dialog.getButton(i);
			if (button.getText().equals(text)) {
				sendMouseClick(button, button.getWidth() / 2, button.getHeight() / 2);
				return true;
			}
		}
		return false;
	}
	
	public static void resetAllCheckBox(View view, boolean isChecked) {
		if (view instanceof ViewGroup) {
			ViewGroup viewGroup = (ViewGroup) view;
			for (int i = 0; i < viewGroup.getChildCount(); i++) {
				resetAllCheckBox(viewGroup.getChildAt(i), isChecked);
			}
		} else {
			if (view instanceof CheckBox) {
				CheckBox checkbox = (CheckBox) view;
				checkbox.setChecked(isChecked);
			}
		}
	}
	
	/*
	 * Enum
	 */
	
	public static interface EnumerateCallback {
		void doAction(Object dialogOrActivity);
	}
	
	@SuppressWarnings("unchecked")
	public static void enumerateViews(EnumerateCallback callback) {
		try {
			Class<?> clazzWindowManagerGlobal = Context.class.getClassLoader().loadClass("android.view.WindowManagerGlobal");
			Method getInstanceMethod = clazzWindowManagerGlobal.getMethod("getInstance", new Class[] {});

			Object instanceWindowManagerGlobal = getInstanceMethod.invoke(clazzWindowManagerGlobal, new Object[] {});
			Field mViewsFields = clazzWindowManagerGlobal.getDeclaredField("mViews");
			mViewsFields.setAccessible(true);

			ArrayList<View> mViews = (ArrayList<View>) mViewsFields.get(instanceWindowManagerGlobal);
			if (mViews != null && mViews.size() > 0) {
				for (int i = mViews.size() - 1; i >= 0; i--) {
					try {
						View view = mViews.get(i);
						if (view.isShown() == false) {
							continue;
						}

						Class<?> clazzDecorView = Context.class.getClassLoader().loadClass("com.android.internal.policy.impl.PhoneWindow$DecorView");
						if (clazzDecorView.isInstance(view)) {
							Field field = clazzDecorView.getDeclaredField("this$0");
							field.setAccessible(true);
							Object value = field.get(view);

							clazzDecorView = Context.class.getClassLoader().loadClass("com.android.internal.policy.impl.PhoneWindow");
							Method getCallbackMethod = clazzDecorView.getMethod("getCallback", new Class[] {});
							getCallbackMethod.setAccessible(true);
							Object callbackObject = getCallbackMethod.invoke(value, new Object[] {});

							if ((callbackObject instanceof Dialog) || (callbackObject instanceof Activity)) {
								callback.doAction(callbackObject);
							} else {
								callbackObject = searchActivityOrDialogIn(callbackObject);
								if ((callbackObject instanceof Dialog) || (callbackObject instanceof Activity)) {
									callback.doAction(callbackObject);
								}
							}
						}

					} catch (Exception e) {
						// do nothing ...
					}
				}
			}

		} catch (Exception e) {
			// do nothing ...
		}

	}

	private static Object searchActivityOrDialogIn(Object object) {
		LinkedList<Object> objectSearched = new LinkedList<Object>();
		return searchActivityOrDialogIn(object, objectSearched);
	}

	private static Object searchActivityOrDialogIn(Object object, LinkedList<Object> searchedRepository) {
		searchedRepository.add(object);
		try {
			Field[] fields = object.getClass().getDeclaredFields();
			if (fields != null) {
				for (int i = 0; i < fields.length; i++) {
					Field field = fields[i];
					field.setAccessible(true);
					Object value = field.get(object);

					if (value != null) {

						if (searchedRepository.indexOf(value) == -1) {
							if (value instanceof Activity || value instanceof Dialog) {
								return value;
							}
							Object result = searchActivityOrDialogIn(value, searchedRepository);
							if (result != null) {
								return result;
							}
						}
					}
				}
			}

		} catch (Exception e) {
			// do nothing ...
		}
		return null;
	}
	
	/*
	 * Print View
	 */

	// adb shell uiautomator dump
	public static void printAllActivityView(Activity activity, boolean printShownOnly) {
		printAllShownView(activity.getWindow().getDecorView(), 0, printShownOnly);
	}

	public static void printAllShownView(View view, int level, boolean printShownOnly) {
		if (view == null) {
			return;
		}

		if (printShownOnly == false || (printShownOnly == true && view.isShown())) {

			String prefixHeader = "";
			for (int i = 0; i < level; i++) {
				prefixHeader = prefixHeader + "--";
			}

			String log = prefixHeader + " " + view.toString() + " " + Integer.toHexString(view.getId()) + " " + view.getLeft() + " " + view.getTop()
					+ " " + getLeftFromScreen(view) + " " + getTopFromScreen(view) + " "
					+ (view instanceof TextView ? ((TextView) view).getText() : "");

			Log.d("View Hierarchy", log);

			if (view instanceof ViewGroup) {
				ViewGroup viewGroup = (ViewGroup) view;
				for (int j = 0; j < viewGroup.getChildCount(); j++) {
					printAllShownView(viewGroup.getChildAt(j), level + 1, printShownOnly);
				}
			}
		}

	}

	public static void printAllShownTextView(View view) {
		printAllShownTextView(view, 0);
	}

	public static void printAllShownTextView(View view, int level) {
		if (view == null) {
			return;
		}

		if (view.isShown() && view instanceof TextView) {

			String prefixHeader = "";
			for (int i = 0; i < level; i++) {
				prefixHeader = prefixHeader + "--";
			}
			String log = prefixHeader + " " + view.toString() + " " + Integer.toHexString(view.getId()) + " " + view.getLeft() + " " + view.getTop()
					+ " " + getLeftFromScreen(view) + " " + getTopFromScreen(view) + " text:" + ((TextView) view).getText().toString();

			Log.d("TextView Hierarchy", log);

			if (view instanceof ViewGroup) {
				ViewGroup viewGroup = (ViewGroup) view;
				for (int j = 0; j < viewGroup.getChildCount(); j++) {
					printAllShownTextView(viewGroup.getChildAt(j), level + 1);
				}
			}
		}

	}

	public static void printallDialogViewWithText(Dialog dialog) {
		if (dialog == null) {
			return;
		}
		printAllShownTextView(dialog.getWindow().getDecorView(), 0);
	}

}
