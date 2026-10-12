# EnglishCoach Android - ProGuard / R8 Rules
# Specific, minimal keep rules preserving runtime integrity without disabling optimizations.

# 1. Room Database
# Keep entity class members (getters, setters, fields, constructors) so Room's generated
# database adapters and cursor mappings can read/write entity state.
-keepclassmembers class * {
    @androidx.room.Entity *;
}
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.RoomDatabase {
    <init>();
}
-dontwarn androidx.room.paging.**

# 2. Android Lifecycle & ViewModel
# Keep public constructors for ViewModels created via ViewModelProvider
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# 3. Google Play Billing Client Callbacks
# AAR consumer rules in billing-9.1.0.aar already retain AIDL interfaces and Proxy activities.
# Only retain listener callback methods invoked asynchronously by Play Store IPC service.
-keepclassmembers class * implements com.android.billingclient.api.BillingClientStateListener {
    public void onBillingSetupFinished(com.android.billingclient.api.BillingResult);
    public void onBillingServiceDisconnected();
}
-keepclassmembers class * implements com.android.billingclient.api.PurchasesUpdatedListener {
    public void onPurchasesUpdated(com.android.billingclient.api.BillingResult, java.util.List);
}

# 4. Text-To-Speech (TTS)
-keepclassmembers class com.andy.englishcoach.util.TtsManager {
    public void onInit(int);
}
