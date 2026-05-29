# Keep the game's own classes (referenced by name in layouts and manifest)
-keep class com.Project.App.Multipong.** { *; }

# AndroidX / AppCompat
-keep class androidx.appcompat.** { *; }
-dontwarn androidx.**

# SpongyCastle (bundled but encryption not yet used — keep to avoid stripping issues)
-keep class com.madgag.spongycastle.** { *; }
-dontwarn com.madgag.spongycastle.**

# Standard Android rules — keep anything referenced by XML layouts
-keepclassmembers class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
}
