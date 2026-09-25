# ==========================================
# STANDARD ANDROID PROTECTIONS
# ==========================================
-keepattributes *Annotation*
-keepattributes Signature

# ==========================================
# 1. KEEP RULES (Prevent R8 from deleting)
# ==========================================

# ZXing-C++: Protect the JNI (C++ to Kotlin bridge)
-keep class zxingcpp.** { *; }

# BoofCV: Protect reflection, core engine, and math dependencies
-keep class boofcv.** { *; }
-keep class georegression.** { *; }
-keep class org.ddogleg.** { *; }

# ==========================================
# 2. WARNING SUPPRESSIONS (Tell R8 to ignore)
# ==========================================

# BoofCV: Ignore missing Desktop Java classes and I/O packages
-dontwarn java.awt.**
-dontwarn java.awt.image.**
-dontwarn javax.imageio.**
-dontwarn javax.swing.**
-dontwarn java.lang.reflect.InaccessibleObjectException
-dontwarn boofcv.io.**

# BoofCV Dependencies: Ignore missing SnakeYAML and Desktop Beans
-dontwarn java.beans.**
-dontwarn org.yaml.snakeyaml.**

# BoofCV Dependencies: Ignore missing compile-time annotations
-dontwarn lombok.**
