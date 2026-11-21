package in.ac.bitspilani.triagegraph.topology.rca.domain;

import java.time.Instant;

public record ChangeInfo(long serviceId, String description, Instant loggedAt) {
}
