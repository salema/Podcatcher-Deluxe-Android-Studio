# This is only here as a reminder.
# If you pull this library update your proguard config to include:

# Prevent proguard from deleting the Retrofit stuff
-dontwarn retrofit.**
-keep class retrofit.** { *; }
-keepattributes Signature
-keepattributes Exceptions