package in.ac.bitspilani.triagegraph.shared.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentAction;
import in.ac.bitspilani.triagegraph.shared.exception.CyclicDependencyException;
import in.ac.bitspilani.triagegraph.shared.exception.DuplicateIncidentException;
import in.ac.bitspilani.triagegraph.shared.exception.IncidentNotFoundException;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidStateTransitionException;
import in.ac.bitspilani.triagegraph.shared.exception.LlmUnavailableException;
import in.ac.bitspilani.triagegraph.shared.exception.UnauthorizedOperationException;
import in.ac.bitspilani.triagegraph.shared.exception.WorkloadExceededException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

class GlobalExceptionHandlerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-21T10:00:00Z"), ZoneOffset.UTC);

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler(CLOCK))
                .build();
    }

    @Test
    void mapsEachBranchOfTheTreeToItsStatus() {
        assertThat(GlobalExceptionHandler.statusFor(new IncidentNotFoundException("x"))).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(GlobalExceptionHandler.statusFor(new UnauthorizedOperationException("x"))).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(GlobalExceptionHandler.statusFor(new DuplicateIncidentException("x"))).isEqualTo(HttpStatus.CONFLICT);
        assertThat(GlobalExceptionHandler.statusFor(new CyclicDependencyException("x"))).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(GlobalExceptionHandler.statusFor(new WorkloadExceededException("x"))).isEqualTo(HttpStatus.CONFLICT);
        assertThat(GlobalExceptionHandler.statusFor(new LlmUnavailableException("x"))).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void invalidTransitionBecomes409ProblemDetail() throws Exception {
        mvc.perform(get("/boom"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value(InvalidStateTransitionException.CODE))
                .andExpect(jsonPath("$.detail").value("Cannot CLOSE an incident in state NEW"))
                .andExpect(jsonPath("$.path").value("/boom"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void beanValidationFailureListsFieldErrors() throws Exception {
        mvc.perform(post("/validate").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("title"));
    }

    record Body(@NotBlank @Size(max = 200) String title) {
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/boom")
        String boom() {
            throw new InvalidStateTransitionException("Cannot " + IncidentAction.CLOSE + " an incident in state NEW");
        }

        @PostMapping("/validate")
        String validate(@Valid @RequestBody Body body) {
            return body.title();
        }
    }
}
