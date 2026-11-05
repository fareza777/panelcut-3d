# WebView JavaScript bridge used by the 3D viewer.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.panelcut3d.app.ui.editor.viewer.ViewerBridge { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.panelcut3d.app.**$$serializer { *; }
-keepclassmembers class com.panelcut3d.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.panelcut3d.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room
-keep class * extends androidx.room.RoomDatabase { <init>(); }

-dontwarn org.jetbrains.annotations.**
