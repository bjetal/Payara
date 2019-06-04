/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package fish.payara.enterprise.server.logging;

import com.sun.enterprise.server.logging.LogRotationTimerTask;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Susan Rai
 */
public class PayaraNotificationLogRotationTimer {

    private LogRotationTimerTask rotationTimerTask;
    private ScheduledFuture<?> logRotationFuture;

    private static PayaraNotificationLogRotationTimer instance = new PayaraNotificationLogRotationTimer();

    private PayaraNotificationLogRotationTimer() {
    }

    public static PayaraNotificationLogRotationTimer getInstance() {
        return instance;
    }

    public void startTimer(ScheduledExecutorService scheduledExecutorService, LogRotationTimerTask timerTask) {
        rotationTimerTask = timerTask;
        logRotationFuture = scheduledExecutorService.schedule(
            rotationTimerTask,
            timerTask.getRotationTimerValue(),
            TimeUnit.MILLISECONDS
        );
    }

    public void stopTimer() {
        if (logRotationFuture != null) {
            logRotationFuture.cancel(false);
        }
    }

    public void restartTimer(ScheduledExecutorService scheduledExecutorService) {
        // We will restart the timer only if the timerTask is set which
        // means user has set a value for LogRotation based on Time
        if (logRotationFuture != null) {
            logRotationFuture.cancel(false);
            rotationTimerTask = new LogRotationTimerTask(
                rotationTimerTask.task,
                rotationTimerTask.getRotationTimerValueInMinutes());
            logRotationFuture = scheduledExecutorService.schedule(
                rotationTimerTask,
                rotationTimerTask.getRotationTimerValue(),
                TimeUnit.MILLISECONDS
            );
        }
    }

    public void restartTimerForDayBasedRotation(ScheduledExecutorService scheduledExecutorService) {
        // We will restart the timer only if the timerTask is set which
        // means user has set a value for LogRotation based on Time
        if (logRotationFuture != null) {
            logRotationFuture.cancel(false);
            rotationTimerTask = new  LogRotationTimerTask(
                rotationTimerTask.task,
                60 * 24);
            logRotationFuture = scheduledExecutorService.schedule(
                rotationTimerTask,
                1000 * 60 * 60 * 24, TimeUnit.MILLISECONDS
            );
        }
    }
}