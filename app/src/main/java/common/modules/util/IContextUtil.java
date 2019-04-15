package common.modules.util;

import android.content.Context;
import android.content.Intent;

public class IContextUtil {
	
	public static void startToPhoneHome(Context context) {
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.addCategory(Intent.CATEGORY_HOME);
		context.startActivity(intent);
	} 
	
}
