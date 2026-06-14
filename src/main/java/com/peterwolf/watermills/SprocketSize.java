package com.peterwolf.watermills;

public enum SprocketSize {
	SMALL("small", 1),
	MEDIUM("medium", 2),
	LARGE("large", 3);

	private final String id;
	private final int ratioSize;

	SprocketSize(final String id, final int ratioSize) {
		this.id = id;
		this.ratioSize = ratioSize;
	}

	public String id() {
		return this.id;
	}

	public int ratioSize() {
		return this.ratioSize;
	}
}
