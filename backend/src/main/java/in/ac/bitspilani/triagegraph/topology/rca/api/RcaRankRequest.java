package in.ac.bitspilani.triagegraph.topology.rca.api;

import java.util.Set;

import jakarta.validation.constraints.NotNull;

public record RcaRankRequest(@NotNull Set<@NotNull Long> affectedServiceIds) {
}
