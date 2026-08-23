-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.grizz.countdown.** {
    *** Companion;
}
-keepclasseswithmembers class com.grizz.countdown.** {
    kotlinx.serialization.KSerializer serializer(...);
}
