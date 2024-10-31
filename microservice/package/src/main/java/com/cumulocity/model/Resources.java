package com.cumulocity.model;

import com.cumulocity.model.application.microservice.validation.MinCpu;
import com.cumulocity.model.application.microservice.validation.MinDataSize;
import com.google.common.base.Optional;
import lombok.*;
import org.svenson.JSONProperty;

import jakarta.validation.constraints.Pattern;
import java.util.Map;

import static com.cumulocity.model.Cpu.CPU_VALUE_PATTERN;
import static com.cumulocity.model.Cpu.CPU_ZERO_MILLIS;
import static com.cumulocity.model.DataSize.DATA_SIZE_VALUE_PATTERN;
import static com.cumulocity.model.DataSize.MEMORY_ZERO_MBYTES;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Resources {

    @Getter(onMethod_ = @JSONProperty(ignoreIfNull = true))
    @Pattern(regexp = CPU_VALUE_PATTERN, message = "Microservice manifest cpu value must either decimal number or millicpu value, for example 0.1 or 100m")
    @MinCpu("100m")
    private String cpu;
    @Getter(onMethod_ = @JSONProperty(ignoreIfNull = true))
    @Pattern(regexp = DATA_SIZE_VALUE_PATTERN, message = "Microservice manifest memory invalid, has to be either integer with number of bytes or number with one of the units E, P, T, G, M, K, Ei, Pi, Ti, Gi, Mi, Ki")
    @MinDataSize("10M")
    private String memory;

    @JSONProperty(ignore = true)
    public Optional<Cpu> getCpuLimit() {
        return Cpu.tryParse(cpu);
    }


    @JSONProperty(ignore = true)
    public Optional<DataSize> getMemoryLimit(){
        return DataSize.tryParse(memory);
    }

    public static Resources empty() {
        return Resources.builder().cpu(CPU_ZERO_MILLIS).memory(MEMORY_ZERO_MBYTES).build();
    }

    public static Resources fromMap(Map<String, Object> map) {
        return Resources.builder()
                .cpu(getValueFromMap(map, "cpu", CPU_ZERO_MILLIS))
                .memory(getValueFromMap(map, "memory", MEMORY_ZERO_MBYTES))
                .build();
    }

    private static String getValueFromMap(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value != null) {
            return value.toString();
        }
        return defaultValue;
    }
}
