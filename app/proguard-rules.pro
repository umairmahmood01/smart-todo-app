# Keep Room generated implementations reachable through reflection-free codegen.
-keep class * extends androidx.room.RoomDatabase { <init>(); }

# Kotlin metadata is required by reflection-based libraries at runtime.
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault
