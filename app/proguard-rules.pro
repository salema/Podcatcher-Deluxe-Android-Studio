# Add project specific ProGuard rules here

# We are Open Source anyway and not obfuscating makes
# error reports so much more enjoyable...
-dontobfuscate

# Prevent proguard from deleting the Dropbox stuff
-keepclassmembers class com.dropbox.core.** { @com.fasterxml.jackson.annotation.JsonCreator *; }
-keepattributes *Annotation*,EnclosingMethod,InnerClasses,Signature
-keepnames class com.fasterxml.jackson.** { *; }
-dontwarn com.dropbox.**
-dontwarn com.fasterxml.jackson.databind.**
-adaptresourcefilenames com/dropbox/core/http/trusted-certs.raw

# Prevent ProGuard from deleting Retrofit stuff
-dontwarn retrofit2.**
-dontwarn okio.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Finally, this is needed for Picasso
-dontwarn com.squareup.okhttp.**