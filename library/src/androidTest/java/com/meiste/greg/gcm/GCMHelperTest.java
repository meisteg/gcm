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
package com.meiste.greg.gcm;

import android.content.Context;

import com.meiste.greg.gcm.shadows.ShadowGoogleCloudMessaging;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, shadows=ShadowGoogleCloudMessaging.class)
public class GCMHelperTest {

    private static final String GCM_SENDER_ID = "123abc";
    private static final String GCM_REG_ID = "abc123";
    private static final long TEST_ON_SERVER_LIFESPAN_MS = 50;
    private static final long PREFS_SET_DELAY_MS = 10;

    @Before public void setup() {
        ShadowGoogleCloudMessaging.setFakeRegId(GCM_REG_ID);
    }

    @Test public void isRegistered() {
        assertThat(GCMHelper.isRegistered(Robolectric.application)).isFalse();
        GCMHelper.storeRegistrationId(Robolectric.application, GCM_REG_ID);
        assertThat(GCMHelper.isRegistered(Robolectric.application)).isTrue();
    }

    @Test public void isRegisteredOnServer() {
        assertThat(GCMHelper.isRegisteredOnServer(Robolectric.application)).isFalse();

        GCMHelper.setRegisteredOnServer(Robolectric.application, true);
        assertThat(GCMHelper.isRegisteredOnServer(Robolectric.application)).isTrue();

        GCMHelper.setRegisteredOnServer(Robolectric.application, false);
        assertThat(GCMHelper.isRegisteredOnServer(Robolectric.application)).isFalse();
    }

    @Test public void getRegistrationId() {
        assertThat(GCMHelper.getRegistrationId(Robolectric.application)).isEmpty();
        GCMHelper.storeRegistrationId(Robolectric.application, GCM_REG_ID);
        assertThat(GCMHelper.getRegistrationId(Robolectric.application)).isEqualTo(GCM_REG_ID);
    }

    @Test public void registerOnServerLifespan() {
        assertThat(GCMHelper.getRegisterOnServerLifespan(Robolectric.application))
                .isEqualTo(GCMHelper.DEFAULT_ON_SERVER_LIFESPAN_MS);

        GCMHelper.setRegisterOnServerLifespan(Robolectric.application, TEST_ON_SERVER_LIFESPAN_MS);
        assertThat(GCMHelper.getRegisterOnServerLifespan(Robolectric.application))
                .isEqualTo(TEST_ON_SERVER_LIFESPAN_MS);
    }

    @Test public void isRegisteredOnServerExpires() throws InterruptedException {
        GCMHelper.setRegisterOnServerLifespan(Robolectric.application, TEST_ON_SERVER_LIFESPAN_MS);
        GCMHelper.setRegisteredOnServer(Robolectric.application, true);

        assertThat(GCMHelper.isRegisteredOnServer(Robolectric.application)).isTrue();
        Thread.sleep(TEST_ON_SERVER_LIFESPAN_MS * 2);
        assertThat(GCMHelper.isRegisteredOnServer(Robolectric.application)).isFalse();
    }

    @Test public void registerSuccess() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        GCMHelper.register(Robolectric.application, GCM_SENDER_ID, new GCMHelper.OnGcmRegistrationListener() {
            @Override
            public boolean onSendRegistrationIdToBackend(final Context context, final String regId)
                    throws IOException {
                latch.countDown();
                return true;
            }
        });
        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
        Thread.sleep(PREFS_SET_DELAY_MS);
        assertThat(GCMHelper.isRegistered(Robolectric.application)).isTrue();
        assertThat(GCMHelper.isRegisteredOnServer(Robolectric.application)).isTrue();
        assertThat(GCMHelper.getRegistrationId(Robolectric.application)).isEqualTo(GCM_REG_ID);
    }

    @Test public void registerFailure() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        GCMHelper.register(Robolectric.application, GCM_SENDER_ID, new GCMHelper.OnGcmRegistrationListener() {
            @Override
            public boolean onSendRegistrationIdToBackend(final Context context, final String regId)
                    throws IOException {
                latch.countDown();
                return false;
            }
        });
        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
        Thread.sleep(PREFS_SET_DELAY_MS);
        assertThat(GCMHelper.isRegistered(Robolectric.application)).isTrue();
        assertThat(GCMHelper.isRegisteredOnServer(Robolectric.application)).isFalse();
        assertThat(GCMHelper.getRegistrationId(Robolectric.application)).isEqualTo(GCM_REG_ID);
    }

    @Test public void registerIfNeededYes() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        GCMHelper.registerIfNeeded(Robolectric.application, GCM_SENDER_ID,
                new GCMHelper.OnGcmRegistrationListener() {
            @Override
            public boolean onSendRegistrationIdToBackend(final Context context, final String regId)
                    throws IOException {
                latch.countDown();
                return true;
            }
        });
        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
        Thread.sleep(PREFS_SET_DELAY_MS);
        assertThat(GCMHelper.isRegistered(Robolectric.application)).isTrue();
        assertThat(GCMHelper.isRegisteredOnServer(Robolectric.application)).isTrue();
        assertThat(GCMHelper.getRegistrationId(Robolectric.application)).isEqualTo(GCM_REG_ID);
    }

    @Test public void registerIfNeededNo() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        GCMHelper.storeRegistrationId(Robolectric.application, GCM_REG_ID);
        GCMHelper.setRegisteredOnServer(Robolectric.application, true);

        GCMHelper.registerIfNeeded(Robolectric.application, GCM_SENDER_ID,
                new GCMHelper.OnGcmRegistrationListener() {
                    @Override
                    public boolean onSendRegistrationIdToBackend(final Context context, final String regId)
                            throws IOException {
                        latch.countDown();
                        return true;
                    }
                });
        assertThat(latch.await(100, TimeUnit.MILLISECONDS)).isFalse();

        // Verify existing registration was not touched
        assertThat(GCMHelper.isRegistered(Robolectric.application)).isTrue();
        assertThat(GCMHelper.isRegisteredOnServer(Robolectric.application)).isTrue();
        assertThat(GCMHelper.getRegistrationId(Robolectric.application)).isEqualTo(GCM_REG_ID);
    }
}
