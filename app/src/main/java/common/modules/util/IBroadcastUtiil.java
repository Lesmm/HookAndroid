package common.modules.util;

import java.util.Iterator;
import java.util.Map;

import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class IBroadcastUtiil {

	public static void send(Context context, String action, JSONObject parameters) {
		Intent intent = new Intent(action);
		
		HLog.log("-------> apk Broadcast command send: " + action + " -> " + parameters.toString());
		
		Bundle bundle = new Bundle();
		@SuppressWarnings("unchecked")
		Map<String, Object> mMap = (Map<String, Object>) IReflectUtil.objectFieldValue(bundle, "mMap");
		
		Iterator<?> iterator = parameters.keys();
		while (iterator.hasNext()) {
			String key = (String) iterator.next();
			Object value = parameters.opt(key);
//			intent.putExtra(key, obj);	// need to cast Type ..., so we use the map ...
			mMap.put(key, value);
		}
		
		intent.putExtras(bundle);
		context.sendBroadcast(intent);
	}
}
