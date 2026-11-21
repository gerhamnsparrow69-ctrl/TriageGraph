package in.ac.bitspilani.triagegraph.assignment.domain;

/** Turns a service id into a display name for recommendation reasons. */
@FunctionalInterface
public interface ServiceNameLookup {

    String nameOf(long serviceId);
}
