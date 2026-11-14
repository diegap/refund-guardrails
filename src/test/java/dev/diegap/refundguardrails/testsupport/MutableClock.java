package dev.diegap.refundguardrails.testsupport;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** A clock tests can advance, so token expiry is exercised without sleeping. */
public final class MutableClock extends Clock {

	private Instant instant;

	private final ZoneId zone;

	public MutableClock(Instant start) {
		this(start, ZoneOffset.UTC);
	}

	private MutableClock(Instant start, ZoneId zone) {
		this.instant = start;
		this.zone = zone;
	}

	public void advance(Duration duration) {
		this.instant = this.instant.plus(duration);
	}

	@Override
	public ZoneId getZone() {
		return this.zone;
	}

	@Override
	public Clock withZone(ZoneId zone) {
		return new MutableClock(this.instant, zone);
	}

	@Override
	public Instant instant() {
		return this.instant;
	}

}
