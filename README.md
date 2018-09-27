
$ adb logcat | grep -E "HOOK|Hook|hook|clsMyRom|cldevrom|main|Main|HLog|System.err|Process"
adb pull /system/build.prop ./
(same as out/target/product/cancro/system/build.prop)


https://blog.csdn.net/whu_zhangmin/article/details/37696387


getCompleteVoiceMailNumber return type : class java.lang.String parameters count: 0
Caused by: java.lang.SecurityException: Requires CALL_PRIVILEGED: Neither user 10063 nor current process has android.permission.CALL_PRIVILEGED.



getIsimDomain return type : class java.lang.String parameters count: 0
Caused by: java.lang.SecurityException: Requires READ_PRIVILEGED_PHONE_STATE: Neither user 10063 nor current process has android.permission.READ_PRIVILEGED_PHONE_STATE.


getIsimImpi return type : class java.lang.String parameters count: 0
Caused by: java.lang.SecurityException: Requires READ_PRIVILEGED_PHONE_STATE: Neither user 10063 nor current process has android.permission.READ_PRIVILEGED_PHONE_STATE.




getNetworkClass return type : int parameters count: 1
W/System.err: java.lang.IllegalArgumentException: wrong number of arguments; expected 1, got 0


getNetworkTypeName return type : class java.lang.String parameters count: 1
W/System.err: java.lang.IllegalArgumentException: wrong number of arguments; expected 1, got 0


getPhoneType return type : int parameters count: 1
W/System.err: java.lang.IllegalArgumentException: wrong number of arguments; expected 1, got 0


getTelephonyProperty return type : class java.lang.String parameters count: 3
W/System.err: java.lang.IllegalArgumentException: wrong number of arguments; expected 3, got 0




________________________________________ some issue ________________________________________

https://stackoverflow.com/a/36521856

Error type 3 Error: Activity class {} does not exist. Error while Launching activity
cd {project_dir) && ./gradlew uninstallAll