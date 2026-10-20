# Keep the app entry points. Everything else is safe to shrink.
-keep class com.phoneflipper.app.ui.** { *; }
-keep class com.phoneflipper.app.FlipperApp { *; }
-keepclassmembers class * extends android.app.Activity { public void *(android.view.View); }
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
