LOCAL_PATH:= $(call my-dir)

#===================================================================
include $(CLEAR_VARS)
LOCAL_SRC_FILES := pollfish_3.0.5.jar
LOCAL_MODULE := pollfish-sdk
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := JAVA_LIBRARIES
LOCAL_MODULE_PATH := $(TARGET_OUT)/fake_packages/$(LOCAL_SRC_FILES)
include $(BUILD_PREBUILT)
#===================================================================
