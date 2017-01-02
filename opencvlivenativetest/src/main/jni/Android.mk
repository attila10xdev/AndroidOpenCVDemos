LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

include c:/Dev/OpenCV-3.2-android-sdk/sdk/native/jni/OpenCV.mk

LOCAL_MODULE    := ocv_jni
LOCAL_SRC_FILES := ocv_jni_code.cpp
LOCAL_LDLIBS +=  -llog -ldl

include $(BUILD_SHARED_LIBRARY)
