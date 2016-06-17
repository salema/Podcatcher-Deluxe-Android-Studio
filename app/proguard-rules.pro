# Add project specific ProGuard rules here

# We are Open Source anyway and not obfuscating makes
# error reports so much more enjoyable...
-dontobfuscate

# Suppress Dropbox warnings
-dontwarn okhttp3.**
-dontwarn com.squareup.okhttp.**
-dontwarn com.google.appengine.**
-dontwarn javax.servlet.**

# Prevent ProGuard from deleting Retrofit stuff
-dontwarn retrofit2.**
-dontwarn okio.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions