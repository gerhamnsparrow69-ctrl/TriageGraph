package in.ac.bitspilani.triagegraph.shared.contracts.dto;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.Criticality;

public record ServiceRef(
        long id, String code, String name, Criticality criticality) {
}
