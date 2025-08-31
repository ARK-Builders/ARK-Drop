# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# Keep Hilt classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keepclasseswithmembers class * {
    @dagger.hilt.android.AndroidEntryPoint <methods>;
}

# Keep Compose classes
-keep class androidx.compose.** { *; }
-keep class kotlin.Metadata { *; }

# Keep serialization classes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class dev.arkbuilders.drop.app.**$$serializer { *; }
-keepclassmembers class dev.arkbuilders.drop.app.** {
    *** Companion;
}
-keepclasseswithmembers class dev.arkbuilders.drop.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep JNA classes
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }

# Keep ZXing classes but exclude desktop GUI components
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.client.j2se.**
-dontwarn java.awt.**
-dontwarn javax.swing.**
-dontwarn javax.imageio.**
-dontwarn org.w3c.dom.bootstrap.**

# Exclude ZXing desktop GUI classes completely
-dontnote com.google.zxing.client.j2se.**

# Keep CameraX classes
-keep class androidx.camera.** { *; }

# Keep ML Kit classes
-keep class com.google.mlkit.** { *; }

# Keep file provider classes
-keep class androidx.core.content.FileProvider { *; }

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Fix for missing javax.imageio classes from ZXing
-dontwarn javax.imageio.spi.ImageInputStreamSpi
-dontwarn javax.imageio.spi.ImageOutputStreamSpi
-dontwarn javax.imageio.spi.ImageReaderSpi
-dontwarn javax.imageio.spi.ImageWriterSpi
-dontwarn com.github.jaiimageio.impl.**

# Fix for missing AWT classes from ZXing desktop components
-dontwarn java.awt.Component
-dontwarn java.awt.Container
-dontwarn java.awt.Dimension
-dontwarn java.awt.FlowLayout
-dontwarn java.awt.Graphics2D
-dontwarn java.awt.GraphicsEnvironment
-dontwarn java.awt.HeadlessException
-dontwarn java.awt.Image
-dontwarn java.awt.LayoutManager
-dontwarn java.awt.Window
-dontwarn java.awt.geom.AffineTransform
-dontwarn java.awt.image.BufferedImage
-dontwarn java.awt.image.ImageObserver
-dontwarn java.awt.image.RenderedImage
-dontwarn java.awt.image.WritableRaster

# Fix for missing Swing classes from ZXing desktop components
-dontwarn javax.swing.Icon
-dontwarn javax.swing.ImageIcon
-dontwarn javax.swing.JFileChooser
-dontwarn javax.swing.JFrame
-dontwarn javax.swing.JLabel
-dontwarn javax.swing.JPanel
-dontwarn javax.swing.JTextArea
-dontwarn javax.swing.SwingUtilities
-dontwarn javax.swing.text.JTextComponent

# Suppress warnings for ZXing desktop classes that we don't use on Android
-dontwarn com.google.zxing.client.j2se.GUIRunner
-dontwarn com.google.zxing.client.j2se.BufferedImageLuminanceSource
-dontwarn com.google.zxing.client.j2se.DecodeWorker
-dontwarn com.google.zxing.client.j2se.HtmlAssetTranslator

# Keep only the ZXing classes we actually use for Android
-keep class com.google.zxing.BarcodeFormat { *; }
-keep class com.google.zxing.WriterException { *; }
-keep class com.google.zxing.common.BitMatrix { *; }
-keep class com.google.zxing.qrcode.QRCodeWriter { *; }

# Additional R8 optimizations
-allowaccessmodification
-repackageclasses ''
