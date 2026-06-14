package com.peterwolf.watermills;

public record MechanicalPower(boolean powered, int speed) {
	public static final MechanicalPower OFF = new MechanicalPower(false, 0);

	public static MechanicalPower of(final int speed) {
		return speed <= 0 ? OFF : new MechanicalPower(true, Math.max(1, Math.min(3, speed)));
	}

	public MechanicalPower scale(final int inputSize, final int outputSize) {
		if (!this.powered || outputSize <= 0) {
			return OFF;
		}
		return of(Math.round((float) this.speed * inputSize / outputSize));
	}

	public MechanicalPower strongest(final MechanicalPower other) {
		if (!this.powered) {
			return other;
		}
		if (!other.powered) {
			return this;
		}
		return other.speed > this.speed ? other : this;
	}
}
