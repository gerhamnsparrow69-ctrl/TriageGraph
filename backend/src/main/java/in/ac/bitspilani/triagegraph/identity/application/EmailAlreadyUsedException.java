package in.ac.bitspilani.triagegraph.identity.application;

import in.ac.bitspilani.triagegraph.shared.exception.BusinessRuleException;

public class EmailAlreadyUsedException extends BusinessRuleException {

    public static final String CODE = "EMAIL_IN_USE";

    public EmailAlreadyUsedException(String email) {
        super(CODE, "An account with " + email + " already exists.");
    }
}
