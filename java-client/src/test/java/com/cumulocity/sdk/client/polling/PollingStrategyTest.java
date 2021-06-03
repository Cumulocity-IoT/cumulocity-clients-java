package com.cumulocity.sdk.client.polling;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

public class PollingStrategyTest {
    
    @Test
    public void shouldIterateOverElements() throws Exception {
        
        PollingStrategy strategy = new PollingStrategy(null, TimeUnit.SECONDS, 1L, 2L);
        strategy.setRepeatLast(true);
        
        assertThatCurrentStateIs(strategy, 1000L);
        assertThatCurrentStateIs(strategy, 2000L);
        assertThatCurrentStateIs(strategy, 2000L);
        
        strategy = new PollingStrategy(null, TimeUnit.SECONDS, 1L, 2L);
        strategy.setRepeatLast(false);
        
        assertThatCurrentStateIs(strategy, 1000L);
        assertThatCurrentStateIs(strategy, 2000L);
        assertThatCurrentStateIsNull(strategy);        
    }

    private void assertThatCurrentStateIs(PollingStrategy strategy, Long value) {
        assertThat(strategy.isEmpty()).isFalse();
        assertThat(strategy.peakNext()).isEqualTo(value);
        assertThat(strategy.popNext()).isEqualTo(value);
    }
    
    private void assertThatCurrentStateIsNull(PollingStrategy strategy) {
        assertThat(strategy.isEmpty()).isTrue();
        assertThat(strategy.peakNext()).isNull();
        assertThat(strategy.popNext()).isNull();
    }

}
