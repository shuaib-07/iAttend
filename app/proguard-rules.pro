# kotlinx.serialization: used for type-safe nav routes and the full-DB JSON export/import.
# Without these, R8 strips the generated $$serializer classes it can't see referenced statically.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.iattend.app.**$$serializer { *; }
-keepclassmembers class com.iattend.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.iattend.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room entities/DAOs are also (de)serialized via kotlinx.serialization for export/import,
# and Room itself reflects into entity constructors.
-keep class com.iattend.app.core.data.db.** { *; }
-keep class com.iattend.app.core.datastore.AppSettings { *; }
-keep class com.iattend.app.core.datastore.Profile { *; }
-keep class com.iattend.app.core.data.export.ExportPayload { *; }

# Glance AppWidgets and ActionCallbacks (instantiated via reflection by Android OS / Glance)
-keep class * implements androidx.glance.appwidget.action.ActionCallback { *; }
-keep class androidx.glance.appwidget.action.ActionCallback { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-keep class com.iattend.app.widget.** { *; }
