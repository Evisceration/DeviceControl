LOCAL_PATH:= $(call my-dir)

#===================================================================
# This jar contains api keys. just unzip it and you can even get the
# keys with notepad. However policies state that i may not check the
# api keys in plaintext in source control, that's why the jar exists
#===================================================================
include $(CLEAR_VARS)
LOCAL_SRC_FILES := nameless-proprietary.jar
LOCAL_MODULE := nameless-proprietary
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := JAVA_LIBRARIES
LOCAL_MODULE_PATH := $(TARGET_OUT)/fake_packages/$(LOCAL_SRC_FILES)
include $(BUILD_PREBUILT)
#===================================================================

#===================================================================
include $(CLEAR_VARS)
LOCAL_SRC_FILES := pollfish_4.0.3.jar
LOCAL_MODULE := pollfish-sdk
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := JAVA_LIBRARIES
LOCAL_MODULE_PATH := $(TARGET_OUT)/fake_packages/$(LOCAL_SRC_FILES)
include $(BUILD_PREBUILT)
#===================================================================
