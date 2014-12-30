/*
 * Copyright (C) 2014 Gregory S. Meiste  <http://gregmeiste.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.meiste.greg.gcm.shadows;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.io.IOException;

@Implements(GoogleCloudMessaging.class)
public class ShadowGoogleCloudMessaging {

    private static String sRegId;

    @Implementation
    public String register(String... senderIds) throws IOException {
        return sRegId;
    }

    /**
     * Non-Android accessor that allows the return value of {@link #register(String...)} to be set.
     *
     * @param regId new regId to return
     */
    public static void setFakeRegId(final String regId) {
        sRegId = regId;
    }
}
