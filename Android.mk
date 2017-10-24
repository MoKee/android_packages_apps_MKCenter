#
# Copyright (C) 2014-2017 The MoKee Open Source Project
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_PACKAGE_NAME := MKCenter
LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_JAVA_LIBRARIES := org.apache.http.legacy telephony-common
LOCAL_STATIC_JAVA_LIBRARIES := \
    android-support-v4 \
    android-support-v7-appcompat \
    android-support-v7-preference \
    android-support-v7-recyclerview \
    android-support-v14-preference \
    android-support-design \
    org.mokee.platform.internal \
    volley

LOCAL_STATIC_JAVA_AAR_LIBRARIES := \
    play-services-ads \
    play-services-ads-lite \
    play-services-basement

LOCAL_RESOURCE_DIR := \
    $(LOCAL_PATH)/res \
    frameworks/support/v7/appcompat/res \
    frameworks/support/v7/preference/res \
    frameworks/support/v7/recyclerview/res \
    frameworks/support/v14/preference/res \
    frameworks/support/design/res

LOCAL_AAPT_FLAGS := --auto-add-overlay \
    --extra-packages android.support.v7.appcompat \
    --extra-packages android.support.v7.preference \
    --extra-packages android.support.v7.recyclerview \
    --extra-packages android.support.v14.preference \
    --extra-packages android.support.v17.preference \
    --extra-packages android.support.design \
    --extra-packages com.google.android.gms

LOCAL_PROGUARD_FLAG_FILES := proguard.flags
LOCAL_MODULE_TAGS := optional
LOCAL_PRIVILEGED_MODULE := true
LOCAL_CERTIFICATE := platform

LOCAL_PROGUARD_ENABLED := obfuscation

ifneq ($(INCREMENTAL_BUILDS),)
    LOCAL_PROGUARD_ENABLED := disabled
    LOCAL_JACK_ENABLED := incremental
    LOCAL_DX_FLAGS := --multi-dex
    LOCAL_JACK_FLAGS := --multi-dex native
endif

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)

PLAY_VERSION := 10.2.1
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := \
    play-services-ads:../../../external/google/play-services-ads/$(PLAY_VERSION)/play-services-ads-$(PLAY_VERSION).aar \
    play-services-ads-lite:../../../external/google/play-services-ads-lite/$(PLAY_VERSION)/play-services-ads-lite-$(PLAY_VERSION).aar \
    play-services-basement:../../../external/google/play-services-basement/$(PLAY_VERSION)/play-services-basement-$(PLAY_VERSION).aar

include $(BUILD_MULTI_PREBUILT)
