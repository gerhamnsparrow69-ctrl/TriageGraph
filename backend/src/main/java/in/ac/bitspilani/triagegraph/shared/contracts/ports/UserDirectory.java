package in.ac.bitspilani.triagegraph.shared.contracts.ports;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.TechnicianProfile;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.UserRef;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;
import java.util.List;
import java.util.Optional;

/** Provided by Member 4 (identity). */
public interface UserDirectory {

    /** @throws in.ac.bitspilani.triagegraph.shared.exception.UserNotFoundException unknown id */
    UserRef user(long id);

    List<TechnicianProfile> technicians();

    Optional<UserRef> teamLeadOf(long technicianId);

    List<UserRef> withRole(Role role);
}
