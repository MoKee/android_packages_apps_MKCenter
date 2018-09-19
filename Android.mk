#
# Copyright (C) 2014-2018 The MoKee Open Source Project
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

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_RESOURCE_DIR := \
    $(LOCAL_PATH)/res

LOCAL_JAVA_LIBRARIES := org.apache.http.legacy telephony-common

LOCAL_STATIC_ANDROID_LIBRARIES := \
    android-support-v4 \
    android-support-v7-appcompat \
    android-support-v7-preference \
    android-support-v7-recyclerview \
    android-support-v14-preference \
    android-support-customtabs \
    android-support-design \
    play-services-ads \
    play-services-ads-base \
    play-services-ads-identifier \
    play-services-ads-lite \
    play-services-basement \
    play-services-gass

LOCAL_STATIC_JAVA_LIBRARIES := \
    org.mokee.platform.internal \
    volley

LOCAL_USE_AAPT2 := true

PLAY_VERSION := 15.0.1
LOCAL_FULL_LIBS_MANIFEST_FILES := \
    $(LOCAL_PATH)/../../../external/google-play-services/play-services-ads-lite/$(PLAY_VERSION)/AndroidManifest.xml \
    $(LOCAL_PATH)/../../../external/google-play-services/play-services-basement/$(PLAY_VERSION)/AndroidManifest.xml

LOCAL_PACKAGE_NAME := MKCenter
LOCAL_CERTIFICATE := platform
LOCAL_MODULE_TAGS := optional
LOCAL_PRIVILEGED_MODULE := true
LOCAL_PRIVATE_PLATFORM_APIS := true

LOCAL_PROGUARD_ENABLED := obfuscation
LOCAL_PROGUARD_FLAG_FILES := proguard.flags

ifneq ($(INCREMENTAL_BUILDS),)
    LOCAL_PROGUARD_ENABLED := disabled
    LOCAL_JACK_ENABLED := incremental
    LOCAL_DX_FLAGS := --multi-dex
    LOCAL_JACK_FLAGS := --multi-dex native
endif

include $(BUILD_PACKAGE)