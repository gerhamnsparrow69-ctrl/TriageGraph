package in.ac.bitspilani.triagegraph.incident.domain;

import in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;
import java.util.Objects;

/** The reporter's 1–5 star rating after closure. */
@Embeddable
public class Feedback {

    public static final int MIN_RATING = 1;
    public static final int MAX_RATING = 5;

    @Column(name = "feedback_rating")
    private Integer rating;

    @Column(name = "feedback_comment", length = 1000)
    private String comment;

    @Column(name = "feedback_at")
    private Instant submittedAt;

    protected Feedback() {
    }

    public Feedback(int rating, String comment, Instant submittedAt) {
        if (rating < MIN_RATING || rating > MAX_RATING) {
            throw new InvalidInputException("Rating must be between 1 and 5.");
        }
        this.rating = rating;
        this.comment = comment;
        this.submittedAt = Objects.requireNonNull(submittedAt);
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }
}
