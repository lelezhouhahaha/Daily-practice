task20196-0418--ok-----Android11上编译ok，运行ok
task20196-0421-all-ok---Android12上编译ok，目前没有设备所以暂时不能验证。
目前看Android11与Android12在添加自定jar包时有一定不一样，Android12中不需要添加PRODUCT_BOOT_JARS，且需要将自定义的jar包包名添加到白名单中
具体修改如下：
diff --git a/build/soong/scripts/check_boot_jars/package_allowed_list.txt b/build/soong/scripts/check_boot_jars/package_allowed_list.txt
index 3dc9847..f94b349 100644
--- a/build/soong/scripts/check_boot_jars/package_allowed_list.txt
+++ b/build/soong/scripts/check_boot_jars/package_allowed_list.txt
@@ -246,6 +246,9 @@ com\.google\.i18n\.phonenumbers
 # Packages used for Android in Chrome OS
 org\.chromium\.arc
 org\.chromium\.arc\..*
+####
+com\.elotouch\.library
+com\.elotouch\.library\..*
 
 # QC adds
 com.qualcomm.qti

可供第三方app开发用的jar包路径如下：
out\soong\.intermediates\packages\modules\elotouch\eloTouchManager\android_common\turbine-combined\eloTouchManager.jar