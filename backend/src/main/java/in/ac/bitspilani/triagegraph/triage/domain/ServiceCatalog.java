package in.ac.bitspilani.triagegraph.triage.domain;

import java.util.List;
import java.util.Optional;

/**
 * The services and categories triage can suggest. Backed by Member 3's catalog when it is deployed; until
 * then service ids are unknown (null) but names and categories still work.
 */
public interface ServiceCatalog {

    record ServiceInfo(Long id, String name) {
    }

    List<ServiceInfo> services();

    List<String> categoryCodes();

    /** Case- and punctuation-insensitive lookup, e.g. "dns dhcp" finds "DNS/DHCP". */
    Optional<ServiceInfo> byName(String name);

    Optional<ServiceInfo> byId(long id);
}
