# Keep Drive API model classes (reflection-based JSON parsing)
-keep class com.google.api.services.drive.model.** { *; }
-keep class com.google.api.client.** { *; }
-dontwarn com.google.api.client.**
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault
