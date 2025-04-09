# avoid protobuf generated classes to be renamed
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}
