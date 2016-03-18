# Add project specific ProGuard rules here

# We are Open Source anyway and not obfuscating makes
# error reports so much more enjoyable...
-dontobfuscate

# Prevent ProGuard from deleting Dropbox stuff
-keep class com.dropbox.** {*;}
-dontwarn com.dropbox.ledger.**

# Prevent ProGuard from deleting Retrofit stuff
-dontwarn retrofit2.**
-dontwarn okio.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Finally, this is needed for Picasso
-dontwarn com.squareup.okhttp.**