package in.ac.bitspilani.triagegraph.shared.contracts.events;

public sealed interface IncidentEvent extends DomainEvent permits IncidentReported, IncidentStatusChanged,
        PriorityAssigned, IncidentAssigned, NoteAdded, SlaWarningRaised, SlaBreached, IncidentEscalated,
        DuplicateLinked, FeedbackSubmitted {

    long incidentId();

    String incidentCode();
}
