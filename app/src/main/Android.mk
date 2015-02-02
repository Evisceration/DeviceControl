LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_STATIC_JAVA_LIBRARIES := \
    android-support-v13 \
    android-support-v7-appcompat \
    android-support-v7-cardview \
    android-support-v7-recyclerview \
    ion \
    nameless-proprietary \
    play \
    pollfish-sdk \

LOCAL_ASSET_DIR := \
    $(LOCAL_PATH)/../../../base/src/main/assets \

LOCAL_RESOURCE_DIR := \
    $(LOCAL_PATH)/res \
    $(LOCAL_PATH)/../../../base/src/main/res \

LOCAL_SRC_FILES    := \
    $(call all-java-files-under,java) \
    $(call all-java-files-under,../../../base/src/main/java) \
    ../../../base/src/main/aidl/com/android/vending/billing/IInAppBillingService.aidl \

LOCAL_AAPT_INCLUDE_ALL_RESOURCES := true

## google-play-services

LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/../../../../../../external/google/google_play_services/libproject/google-play-services_lib/res

## android-support-v7-appcompat

LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/../../../../../../frameworks/support/v7/appcompat/res

## android-support-v7-cardview

LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/../../../../../../frameworks/support/v7/cardview/res

## MPAndroidChart

library_src_files := ../../../../../../external/mpandroidchart/MPChartLib/src
LOCAL_SRC_FILES   += $(call all-java-files-under, $(library_src_files))

LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/../../../../../../external/mpandroidchart/MPChartLib/res

######

LOCAL_AAPT_FLAGS := \
    --auto-add-overlay \
    --extra-packages android.support.v7.appcompat \
    --extra-packages android.support.v7.cardview \
    --extra-packages com.github.mikephil.charting \
    --extra-packages com.google.android.gms \

# DeviceControl modules
LOCAL_AAPT_FLAGS += \
    --extra-packages org.namelessrom.devicecontrol.base \
    --extra-packages org.namelessrom.proprietary \

######

LOCAL_PROGUARD_FLAG_FILES := proguard.pro

LOCAL_PACKAGE_NAME      := DeviceControl
LOCAL_CERTIFICATE       := platform
LOCAL_PRIVILEGED_MODULE := true
LOCAL_MODULE_TAGS       := optional

include $(BUILD_PACKAGE)

include $(call all-makefiles-under,$(LOCAL_PATH))
