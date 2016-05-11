LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

include s:/Android/OpenCV-android-sdk/sdk/native/jni/OpenCV.mk

LOCAL_MODULE    := ocv_jni
LOCAL_SRC_FILES := ocv_jni_code.cpp
LOCAL_LDLIBS +=  -llog -ldl

include $(BUILD_SHARED_LIBRARY)
