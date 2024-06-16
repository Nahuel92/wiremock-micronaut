package app;

import org.assertj.core.api.AutoCloseableSoftAssertions;

public class CommonAssertions {
    public static AutoCloseableSoftAssertions assertThatUserHasIdAndName(final User result, long id, final String name) {
        final var softly = new AutoCloseableSoftAssertions();
        softly.assertThat(result).isNotNull();
        softly.assertThat(result.id()).isEqualTo(id);
        softly.assertThat(result.name()).isEqualTo(name);
        return softly;
    }
}