package in.ac.bitspilani.triagegraph.notification.domain;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.UserRef;

public record Recipient(UserRef user, RecipientRole role) {
}
