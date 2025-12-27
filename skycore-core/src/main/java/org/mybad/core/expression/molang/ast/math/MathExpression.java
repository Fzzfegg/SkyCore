package org.mybad.core.expression.molang.ast.math;

import org.mybad.core.expression.molang.Molang;
import org.mybad.core.expression.molang.ast.MolangExpression;
import org.mybad.core.expression.molang.reference.ExpressionBindingContext;

public class MathExpression {

    public static class Abs implements MolangExpression {
        private final MolangExpression input;

        public Abs(MolangExpression input) {
            this.input = input;
        }

        public MolangExpression getInput() {
            return input;
        }

        @Override
        public float evaluate() {
            return Math.abs(input.evaluate());
        }

        @Override
        public MolangExpression simplify() {
            return new Abs(input.simplify());
        }

        @Override
        public MolangExpression bind(ExpressionBindingContext context, Object... values) {
            return new Abs(input.bind(context, values));
        }
    }

    public static class Acos implements MolangExpression {
        private final MolangExpression input;

        public Acos(MolangExpression input) {
            this.input = input;
        }

        public MolangExpression getInput() {
            return input;
        }

        @Override
        public float evaluate() {
            return (float) Math.acos(Math.toRadians(input.evaluate()));
        }

        @Override
        public MolangExpression simplify() {
            return new Acos(input.simplify());
        }

        @Override
        public MolangExpression bind(ExpressionBindingContext context, Object... values) {
            return new Acos(input.bind(context, values));
        }
    }

    public static class Asin implements MolangExpression {
        private final MolangExpression input;

        public Asin(MolangExpression input) {
            this.input = input;
        }

        public MolangExpression getInput() {
            return input;
        }

        @Override
        public float evaluate() {
            return (float) Math.asin(Math.toRadians(input.evaluate()));
        }

        @Override
        public MolangExpression simplify() {
            return new Asin(input.simplify());
        }

        @Override
        public MolangExpression bind(ExpressionBindingContext context, Object... values) {
            return new Asin(input.bind(context, values));
        }
    }

    public static class Atan implements MolangExpression {
        private final MolangExpression input;

        public Atan(MolangExpression input) {
            this.input = input;
        }

        public MolangExpression getInput() {
            return input;
        }

        @Override
        public float evaluate() {
            return (float) Math.atan(Math.toRadians(input.evaluate()));
        }

        @Override
        public MolangExpression simplify() {
            return new Atan(input.simplify());
        }

        @Override
        public MolangExpression bind(ExpressionBindingContext context, Object... values) {
            return new Atan(input.bind(context, values));
        }
    }

    public static class Atan2 implements MolangExpression {
        private final MolangExpression y;
        private final MolangExpression x;

        public Atan2(MolangExpression y, MolangExpression x) {
            this.y = y;
            this.x = x;
        }

        public MolangExpression getY() {
            return y;
        }

        public MolangExpression getX() {
            return x;
        }

        @Override
        public float evaluate() {
            return (float) Math.atan2(Math.toRadians(y.evaluate()), Math.toRadians(x.evaluate()));
        }

        @Override
        public MolangExpression simplify() {
            return new Atan2(y.simplify(), x.simplify());
        }

        @Override
        public MolangExpression bind(ExpressionBindingContext context, Object... values) {
            return new Atan2(y.bind(context, values), x.bind(context, values));
        }
    }

    public static class Ceil implements MolangExpression {
        private final MolangExpression input;

        public Ceil(MolangExpression input) {
            this.input = input;
        }

        public MolangExpression getInput() {
            return input;
        }

        @Override
        public float evaluate() {
            return (float) Math.ceil(input.evaluate());
        }

        @Override
        public MolangExpression simplify() {
            return new Ceil(input.simplify());
        }

        @Override
        public MolangExpression bind(ExpressionBindingContext context, Object... values) {
            return new Ceil(input.bind(context, values));
        }
    }

    public static class Clamp implements MolangExpression {
        private final MolangExpression input;
        private final MolangExpression min;
        private final MolangExpression max;

        public Clamp(MolangExpression input, MolangExpression min, MolangExpression max) {
            this.input = input;
            this.min = min;
            this.max = max;
        }

        public MolangExpression getInput() {
            return input;
        }

        public MolangExpression getMin() {
            return min;
        }

        public MolangExpression getMax() {
            return max;
        }

        @Override
        public float evaluate() {
            float value = input.evaluate();
            return Math.min(Math.max(value, min.evaluate()), max.evaluate());
        }

        @Override
        public MolangExpression simplify() {
            return new Clamp(input.simplify(), min.simplify(), max.simplify());
        }

        @Override
        public MolangExpression bind(ExpressionBindingContext context, Object... values) {
            return new Clamp(input.bind(context, values), min.bind(context, values), max.bind(context, values));
        }
    }

    public static class Cos implements MolangExpression {
        private final MolangExpression input;

        public Cos(MolangExpression input) {
            this.input = input;
        }

        public MolangExpression getInput() {
            return input;
        }

        @Override
        public float evaluate() {
            return (float) Math.cos(Math.toRadians(input.evaluate()));
        }

        @Override
        public MolangExpression simplify() {
            return new Cos(input.simplify());
        }

        @Override
        public MolangExpression bind(ExpressionBindingContext context, Object... values) {
            return new Cos(input.bind(context, values));
        }
    }

    public static class DieRoll implements MolangExpression {
        private final MolangExpression num;
        private final MolangExpression low;
        private final MolangExpression high;

        public DieRoll(MolangExpression num, MolangExpression low, MolangExpression high) {
            this.num = num;
            this.low = low;
            this.high = high;
        }

        public MolangExpression getNum() {
            return num;
        }

        public MolangExpression getLow() {
            return low;
        }

        public MolangExpression getHigh() {
            return high;
        }

        @Override
        public float evaluate() {
            int num_rolls = (int) num.evaluate();
            float low_val = low.evaluate();
            float high_val = high.evaluate();
            float result = 0;
            for (int i = 0; i < num_rolls; i++) {
                result += low_val + (Math.random() * (high_val - low_val));
            }
            return result;
        }

        @Override
        public MolangExpression simplify() {
            return new DieRoll(num.simplify(), low.simplify(), high.simplify());
        }

        @Override
        public MolangExpression bind(ExpressionBindingContext context, Object... values) {
            return new DieRoll(num.bind(context, values), low.bind(context, values), high.bind(context, values));
        }
    }

    public static class DieRollInteger implements MolangExpression {
        private final MolangExpression num;
        private final MolangExpression low;
        private final MolangExpression high;

        public DieRollInteger(MolangExpression num, MolangExpression low, MolangExpression high) {
            this.num = num;
            this.low = low;
            this.high = high;
        }

        public MolangExpression getNum() {
            return num;
        }

        public MolangExpression getLow() {
            return low;
        }

        public MolangExpression getHigh() {
            return high;
        }

        @Override
        public float evaluate() {
            int num_rolls = (int) num.evaluate();
            int low_val = (int) low.evaluate();
            int high_val = (int) high.evaluate();
            int result = 0;
            for (int i = 0; i < num_rolls; i++) {
                result += low_val + (int) (Math.random() * (high_val - low_val + 1));
            }
            return result;
        }

        @Override
        public MolangExpression simplify() {
            return new DieRollInteger(num.simplify(), low.simplify(), high.simplify());
        }

        @Override
        public MolangExpression bind(ExpressionBindingContext context, Object... values) {
            return new DieRollInteger(num.bind(context, values), low.bind(context, values), high.bind(context, values));
        }
    }

    public static class Exp implements MolangExpression {
        private final MolangExpression input;

        public Exp(MolangExpression input) {
            this.input = input;
        }

        public MolangExpression getInput() {
            return input;
        }

        @Override
        public float evaluate() {
            return (float) Math.exp(input.evaluate());
        }

        @Override
        public MolangExpression simplify() {
            return new Exp(input.simplify());
        }

        @Override
        public MolangExpression bind(ExpressionBindingContext context, Object... values) {
            return new Exp(input.bind(context, values));
        }
    }

    public static class Floor implements MolangExpression {
        private final MolangExpression input;

        public Floor(MolangExpression input) {
            this.input = input;
        }

        public MolangExpression getInput() {
            return input;
        }

        @Override
        public float evaluate() {
            return (float) Math.floor(input.evaluate());
        }

        @Override
        public MolangExpression simplify() {
            return new Floor(input.simplify());
        }

        @Override
        public MolangExpression bind(ExpressionBindingContext context, Object... values) {
            return new Floor(input.bind(context, values));
        }
    }

    public static class Lerp implements MolangExpression {
        private final MolangExpression start;
        private final MolangExpression end;
        private final MolangExpression amount;

        public Lerp(MolangExpression start, MolangExpression end, MolangExpression amount) {
            this.start = start;
            this.end = end;
            this.amount = amount;
        }

        public MolangExpression getStart() {
            return start;
        }

        public MolangExpression getEnd() {
            return end;
        }

        public MolangExpression getAmount() {
            return amount;
        }

        @Override
        public float evaluate() {
            float start_val = start.evaluate();
            float end_val = end.evaluate();
            float amount_val = amount.evaluate();
            return start_val + (end_val - start_val) * amount_val;
        }

        @Override
        public MolangExpression simplify() {
            return new Lerp(start.simplify(), end.simplify(), amount.simplify());
        }

        @Override
        public MolangExpression bind(ExpressionBindingContext context, Object... values) {
            return new Lerp(start.bind(context, values), end.bind(context, values), amount.bind(context, values));
        }
    }

    public static class Ln implements MolangExpression {
        private final MolangExpression input;

        public Ln(MolangExpression input) {
            this.input = input;
        }

        public MolangExpression getInput() {
            return input;
        }

        @Override
        public float evaluate() {
            return (float) Math.log(input.evaluate());
        }

        @Override
        public MolangExpression simplify() {
            return new Ln(input.simplify());
        }

        @Override
        public MolangExpression bind(ExpressionBindingContext context, Object... values) {
            return new Ln(input.bind(context, values));
        }
    }

    public static class Max implements MolangExpression {
        private final MolangExpression a;
        private final MolangExpression b;

        public Max(MolangExpression a, MolangExpression b) {
            this.a = a;
            this.b = b;
        }

        public MolangExpression getA() {
            return a;
        }

        public MolangExpression getB() {
            return b;
        }

        @Override
        public float evaluate() {
            return Math.max(a.evaluate(), b.evaluate());
        }

        @Override
        public MolangExpression simplify() {
            return new Max(a.simplify(), b.simplify());
        }

        @Override
        public MolangExpression bind(ExpressionBindingContext context, Object... values) {
            return new Max(a.bind(context, values), b.bind(context, values));
        }
    }

    public static class Min implements MolangExpression {
        private final MolangExpression a;
        private final MolangExpression b;

        public Min(MolangExpression a, MolangExpression b) {
            this.a = a;
            this.b = b;
        }

        public MolangExpression getA() {
            return a;
        }

        public MolangExpression getB() {
            return b;
        }

        @Override
        public float evaluate() {
            return Math.min(a.evaluate(), b.evaluate());
        }

        @Override
        public MolangExpression simplify() {
            return new Min(a.simplify(), b.simplify());
        }

        @Override
        public MolangExpression bind(ExpressionBindingContext context, Object... values) {
            return new Min(a.bind(context, values), b.bind(context, values));
        }
    }

    public static class Pow implements MolangExpression {
        private final MolangExpression base;
        private final MolangExpression exponent;

        public Pow(MolangExpression base, MolangExpression exponent) {
            this.base = base;
            this.exponent = exponent;
        }

        public MolangExpression getBase() {
            return base;
        }

        public MolangExpression getExponent() {
            return exponent;
        }

        @Override
        public float evaluate() {
            return (float) Math.pow(base.evaluate(), exponent.evaluate());
        }

        @Override
        public MolangExpression simplify() {
            return new Pow(base.simplify(), exponent.simplify());
        }

        @Override
        public MolangExpression bind(ExpressionBindingContext context, Object... values) {
            return new Pow(base.bind(context, values), exponent.bind(context, values));
        }
    }

    public static class Random implements MolangExpression {
        private final MolangExpression low;
        private final MolangExpression high;

        public Random(MolangExpression low, MolangExpression high) {
            this.low = low;
            this.high = high;
        }

        public MolangExpression getLow() {
            return low;
        }

        public MolangExpression getHigh() {
            return high;
        }

        @Override
        public float evaluate() {
            float low_val = low.evaluate();
            float high_val = high.evaluate();
            return low_val + (float) (Math.random() * (high_val - low_val));
        }

        @Override
        public MolangExpression simplify() {
            return new Random(low.simplify(), high.simplify());
        }

        @Override
        public MolangExpression bind(ExpressionBindingContext context, Object... values) {
            return new Random(low.bind(context, values), high.bind(context, values));
        }
    }

    public static class RandomInteger implements MolangExpression {
        private final MolangExpression low;
        private final MolangExpression high;

        public RandomInteger(MolangExpression low, MolangExpression high) {
            this.low = low;
            this.high = high;
        }

        public MolangExpression getLow() {
            return low;
        }

        public MolangExpression getHigh() {
            return high;
        }

        @Override
        public float evaluate() {
            int low_val = (int) low.evaluate();
            int high_val = (int) high.evaluate();
            return low_val + (int) (Math.random() * (high_val - low_val + 1));
        }

        @Override
        public MolangExpression simplify() {
            return new RandomInteger(low.simplify(), high.simplify());
        }

        @Override
        public MolangExpression bind(ExpressionBindingContext context, Object... values) {
            return new RandomInteger(low.bind(context, values), high.bind(context, values));
        }
    }

    public static class Round implements MolangExpression {
        private final MolangExpression input;

        public Round(MolangExpression input) {
            this.input = input;
        }

        public MolangExpression getInput() {
            return input;
        }

        @Override
        public float evaluate() {
            return Math.round(input.evaluate());
        }

        @Override
        public MolangExpression simplify() {
            return new Round(input.simplify());
        }

        @Override
        public MolangExpression bind(ExpressionBindingContext context, Object... values) {
            return new Round(input.bind(context, values));
        }
    }

    public static class Sin implements MolangExpression {
        private final MolangExpression input;

        public Sin(MolangExpression input) {
            this.input = input;
        }

        public MolangExpression getInput() {
            return input;
        }

        @Override
        public float evaluate() {
            return (float) Math.sin(Math.toRadians(input.evaluate()));
        }

        @Override
        public MolangExpression simplify() {
            return new Sin(input.simplify());
        }

        @Override
        public MolangExpression bind(ExpressionBindingContext context, Object... values) {
            return new Sin(input.bind(context, values));
        }
    }

    public static class Sqrt implements MolangExpression {
        private final MolangExpression input;

        public Sqrt(MolangExpression input) {
            this.input = input;
        }

        public MolangExpression getInput() {
            return input;
        }

        @Override
        public float evaluate() {
            return (float) Math.sqrt(input.evaluate());
        }

        @Override
        public MolangExpression simplify() {
            return new Sqrt(input.simplify());
        }

        @Override
        public MolangExpression bind(ExpressionBindingContext context, Object... values) {
            return new Sqrt(input.bind(context, values));
        }
    }

    public static class Tan implements MolangExpression {
        private final MolangExpression input;

        public Tan(MolangExpression input) {
            this.input = input;
        }

        public MolangExpression getInput() {
            return input;
        }

        @Override
        public float evaluate() {
            return (float) Math.tan(Math.toRadians(input.evaluate()));
        }

        @Override
        public MolangExpression simplify() {
            return new Tan(input.simplify());
        }

        @Override
        public MolangExpression bind(ExpressionBindingContext context, Object... values) {
            return new Tan(input.bind(context, values));
        }
    }

    public static class HermiteBlend implements MolangExpression {
        private final MolangExpression input;

        public HermiteBlend(MolangExpression input) {
            this.input = input;
        }

        public MolangExpression getInput() {
            return input;
        }

        @Override
        public float evaluate() {
            float t = input.evaluate();
            return t * t * (3.0f - 2.0f * t);
        }

        @Override
        public MolangExpression simplify() {
            return new HermiteBlend(input.simplify());
        }

        @Override
        public MolangExpression bind(ExpressionBindingContext context, Object... values) {
            return new HermiteBlend(input.bind(context, values));
        }
    }

    public static class MinAngle implements MolangExpression {
        private final MolangExpression input;

        public MinAngle(MolangExpression input) {
            this.input = input;
        }

        public MolangExpression getInput() {
            return input;
        }

        @Override
        public float evaluate() {
            float angle = input.evaluate();
            while (angle > 180.0f) {
                angle -= 360.0f;
            }
            while (angle < -180.0f) {
                angle += 360.0f;
            }
            return angle;
        }

        @Override
        public MolangExpression simplify() {
            return new MinAngle(input.simplify());
        }

        @Override
        public MolangExpression bind(ExpressionBindingContext context, Object... values) {
            return new MinAngle(input.bind(context, values));
        }
    }

    public static class Mod implements MolangExpression {
        private final MolangExpression a;
        private final MolangExpression b;

        public Mod(MolangExpression a, MolangExpression b) {
            this.a = a;
            this.b = b;
        }

        public MolangExpression getA() {
            return a;
        }

        public MolangExpression getB() {
            return b;
        }

        @Override
        public float evaluate() {
            float divisor = b.evaluate();
            if (divisor == 0.0f) {
                return 0.0f;
            }
            return a.evaluate() % divisor;
        }

        @Override
        public MolangExpression simplify() {
            return new Mod(a.simplify(), b.simplify());
        }

        @Override
        public MolangExpression bind(ExpressionBindingContext context, Object... values) {
            return new Mod(a.bind(context, values), b.bind(context, values));
        }
    }

    public static class Pi implements MolangExpression {
        @Override
        public float evaluate() {
            return (float) Math.PI;
        }

        @Override
        public MolangExpression simplify() {
            return this;
        }

        @Override
        public MolangExpression bind(ExpressionBindingContext context, Object... values) {
            return this;
        }
    }

    public static class Trunc implements MolangExpression {
        private final MolangExpression input;

        public Trunc(MolangExpression input) {
            this.input = input;
        }

        public MolangExpression getInput() {
            return input;
        }

        @Override
        public float evaluate() {
            return (float) Math.signum(input.evaluate()) * (float) Math.floor(Math.abs(input.evaluate()));
        }

        @Override
        public MolangExpression simplify() {
            return new Trunc(input.simplify());
        }

        @Override
        public MolangExpression bind(ExpressionBindingContext context, Object... values) {
            return new Trunc(input.bind(context, values));
        }
    }
}
