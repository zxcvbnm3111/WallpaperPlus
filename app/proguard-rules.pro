# ProGuard rules for WallpaperPlus

# Xposed
-keep class de.robv.android.xposed.** { *; }
-keep class com.wallpaperplus.module.MainHook { *; }
-keep class com.wallpaperplus.module.hook.** { *; }

# Keep all hook classes
-keep class com.wallpaperplus.module.hook.* { *; }

# Keep services and receivers
-keep class com.wallpaperplus.module.service.** { *; }
-keep class com.wallpaperplus.module.receiver.** { *; }

# Keep UI classes
-keep class com.wallpaperplus.module.ui.** { *; }

# Keep util classes
-keep class com.wallpaperplus.module.util.** { *; }

# Don't warn about Xposed
-dontwarn de.robv.android.xposed.**
