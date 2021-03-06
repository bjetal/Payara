/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.monitoring.alert;

import java.util.Objects;

import fish.payara.monitoring.model.SeriesDataset;

/**
 * Describes when a {@link SeriesDataset} {@link #isSatisfied(SeriesDataset)}.
 * 
 * In the simplest form this is a plain comparison with a constant {@link #threshold} value.
 * 
 * More advanced {@link Condition}s check if the condition is satisfied for last number of values in the dataset or for
 * a past number of milliseconds. Such checks either check each included value of the dataset against the threshold
 * (ALL) or compare their average against the threshold in a single check for any number of included values.
 * 
 * @author Jan Berntitt
 */
public final class Condition {

    public static final Condition NONE = new Condition(Operator.EQ, 0L);

    public enum Operator {
        LT("<"), LE("<="), EQ("="), GT(">"), GE(">=");

        public String symbol;

        Operator(String symbol) {
            this.symbol = symbol;
        }

        @Override
        public String toString() {
            return symbol;
        }
    }

    public final Operator comparison;
    public final long threshold;
    public final Number forLast;
    public final boolean onAverage;

    public Condition(Operator comparison, long threshold) {
        this(comparison, threshold, null, false);
    }

    public Condition(Operator comparison, long threshold, Number forLast, boolean onAverage) {
        this.comparison = comparison;
        this.threshold = threshold;
        this.forLast = forLast;
        this.onAverage = onAverage;
    }

    public Condition forLastMillis(long millis) {
        return new Condition(comparison, threshold, millis, false);
    }

    public Condition forLastTimes(int times) {
        return new Condition(comparison, threshold, times, false);
    }

    public Condition onAverage() {
        return new Condition(comparison, threshold, forLast, true);
    }

    public boolean isNone() {
        return this == NONE;
    }

    public boolean isForLastPresent() {
        return isForLastMillis() || isForLastTimes();
    }

    public boolean isForLastMillis() {
        return forLast instanceof Long;
    }

    public boolean isForLastTimes() {
        return forLast instanceof Integer;
    }

    public boolean isSatisfied(SeriesDataset data) {
        if (isNone()) {
            return true;
        }
        if (data.getObservedValues() == 0) {
            return false;
        }
        long value = data.lastValue();
        if ((!onAverage || data.isStable()) && !compare(value)) {
            return false;
        }
        if (isForLastMillis()) {
            return isSatisfiedForLastMillis(data);
        }
        if (isForLastTimes()) {
            return isSatisfiedForLastTimes(data);
        }
        return true;
    }

    private boolean isSatisfiedForLastMillis(SeriesDataset data) {
        long forLastMillis = forLast.longValue();
        if (forLastMillis <= 0) {
            return isSatisfiedForLastTimes(data.points(), -1);
        }
        if (data.isStable()) {
            return data.getStableSince() <= data.lastTime() - forLastMillis ;
        }
        long startTime = data.lastTime() - forLastMillis;
        long[] points = data.points();
        if (points[0] > startTime && forLastMillis < 30000L) {
            return false; // not enough data
        }
        int index = points.length - 2; // last time index
        while (index >= 0 && points[index] > startTime) {
            index -= 2;
        }
        return isSatisfiedForLastTimes(points, index <= 0 ? points.length / 2 : (points.length - index) / 2);
    }

    private boolean isSatisfiedForLastTimes(SeriesDataset data) {
        int forLastTimes = forLast.intValue();
        if (data.isStable()) {
            return data.getStableCount() >= forLastTimes;
        }
        return isSatisfiedForLastTimes(data.points(), forLastTimes);
    }

    private boolean isSatisfiedForLastTimes(long[] points, int forLastTimes) {
        int maxPoints = points.length / 2;
        int n = forLastTimes <= 0 ? maxPoints : Math.min(maxPoints, forLastTimes);
        if (forLastTimes > 0 && n < forLastTimes && n < 30) {
            return false; // not enough data yet
        }
        int index = points.length - 1; // last value index
        if (onAverage) {
            long sum = 0;
            for (int i = 0; i < n; i++) {
                sum += points[index];
                index -= 2;
            }
            return compare(sum / n);
        }
        for (int i = 0; i < n; i++) {
            if (!compare(points[index])) {
                return false;
            }
            index -= 2;
        }
        return true;
    }

    private boolean compare(long value) {
        switch (comparison) {
        default:
        case EQ: return value == threshold;
        case LE: return value <= threshold;
        case LT: return value < threshold;
        case GE: return value >= threshold;
        case GT: return value > threshold;
        }
    }

    @Override
    public int hashCode() {
        return (int) (comparison.hashCode() ^ threshold ^ (forLast == null ? 0 : forLast.intValue())); // good enough to avoid most collisions
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Condition && equalTo((Condition) obj);
    }

    public boolean equalTo(Condition other) {
        return comparison == other.comparison && threshold == other.threshold
                && Objects.equals(forLast, other.forLast) && onAverage == other.onAverage;
    }

    @Override
    public String toString() {
        if (isNone()) {
            return "";
        }
        StringBuilder str = new StringBuilder();
        str.append("value ").append(comparison.toString()).append(' ').append(threshold);
        if (onAverage && isForLastPresent()) {
            str.append(" on average");
        }
        if (isForLastTimes()) {
            str.append(" for last ").append(forLast).append("x");
        }
        if (isForLastMillis()) {
            str.append(" for last ").append(forLast).append("ms");
        }
        return str.toString();
    }

}
