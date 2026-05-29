# Keep the game's own classes (referenced by name in layouts and manifest)
-keep class com.Project.App.Multipong.** { *; }

# AndroidX / AppCompat
-keep class androidx.appcompat.** { *; }
-dontwarn androidx.**

# Standard Android rules — keep anything referenced by XML layouts
-keepclassmembers class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
}
