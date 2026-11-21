package in.ac.bitspilani.triagegraph.shared.contracts.dto;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;

public record UserRef(
        long id, String name, Role role, String email, String pushTopic) {
}
