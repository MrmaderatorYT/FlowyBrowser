# Minimal ProGuard rules for secure browser
# Optimized for small APK size and maximum security

# Keep WebView functionality (no JavaScript interfaces needed)
-keep public class * extends android.webkit.WebViewClient { *; }
-keep public class * extends android.webkit.WebChromeClient { *; }

# Keep main activity and its methods
-keep public class com.ccs.flowybrowser.MainActivity {
    public *;
}

# Keep secure WebView client
-keep public class com.ccs.flowybrowser.SecureWebViewClient {
    public *;
}

# General optimization rules
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose

# Keep line numbers for debugging (remove in production)
-keepattributes SourceFile,LineNumberTable

# SnakeYAML's optional JavaBeans integration is not available on Android.
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.FeatureDescriptor
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor
