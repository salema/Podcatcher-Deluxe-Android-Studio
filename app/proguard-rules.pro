# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /opt/android-studio/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# We are open source and not obfuscating actually makes error report
# so much more enjoyable...
-dontobfuscate

# Prevent proguard from deleting the Dropbox stuff
-keep class com.dropbox.** {*;}
-dontwarn com.dropbox.ledger.**

# Prevent proguard from deleting the Retrofit stuff
-dontwarn retrofit2.**
-dontwarn okio.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# This is needed for Picasso
-dontwarn com.squareup.okhttp.**