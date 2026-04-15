package com.learning.microservice.common.id;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import java.util.UUID;

/** UUIDv7 generator — monotonically increasing by creation time, safe for DB PKs. */
public final class UuidV7 {

  private static final TimeBasedEpochGenerator GEN = Generators.timeBasedEpochGenerator();

  private UuidV7() {}

  public static UUID generate() {
    return GEN.generate();
  }
}
