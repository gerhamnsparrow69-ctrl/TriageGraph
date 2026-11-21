package in.ac.bitspilani.triagegraph.identity.application;

import org.springframework.stereotype.Component;

import in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException;

/** At least 8 characters with at least one letter and one digit (blueprint §11). */
@Component
public class PasswordPolicy {

    public static final int MIN_LENGTH = 8;

    public void check(String password) {
        if (password == null || password.length() < MIN_LENGTH
                || password.chars().noneMatch(Character::isLetter)
                || password.chars().noneMatch(Character::isDigit)) {
            throw new InvalidInputException(
                    "Password must be at least " + MIN_LENGTH + " characters and contain a letter and a digit.");
        }
    }
}
