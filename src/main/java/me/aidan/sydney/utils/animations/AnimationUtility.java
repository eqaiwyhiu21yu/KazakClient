package me.aidan.sydney.utils.animations;

public class AnimationUtility {
    public static float fast(float current, float target, float speed) {
        return current + (target - current) / speed;
    }

    public static double fast(double current, double target, float speed) {
        return current + (target - current) / speed;
    }

    public static float lerp(float start, float end, float delta) {
        return start + (end - start) * delta;
    }
}
