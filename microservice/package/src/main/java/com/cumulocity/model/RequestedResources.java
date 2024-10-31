package com.cumulocity.model;

import com.google.common.base.Optional;
import lombok.*;
import org.svenson.JSONProperty;

import jakarta.validation.constraints.Pattern;

import static com.cumulocity.model.Cpu.CPU_VALUE_PATTERN;
import static com.cumulocity.model.DataSize.DATA_SIZE_VALUE_PATTERN;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RequestedResources {
    @Getter(onMethod_ = @JSONProperty(ignoreIfNull = true))
    @Pattern(regexp = CPU_VALUE_PATTERN, message = "Microservice manifest cpu value must either decimal number or millicpu value, for example 0.1 or 100m")
    private String cpu;
    @Getter(onMethod_ = @JSONProperty(ignoreIfNull = true))
    @Pattern(regexp = DATA_SIZE_VALUE_PATTERN, message = "Microservice manifest memory invalid, has to be either integer with number of bytes or number with one of the units E, P, T, G, M, K, Ei, Pi, Ti, Gi, Mi, Ki")
    private String memory;

    @JSONProperty(ignore = true)
    public Optional<Cpu> getRequestedCpu() {
        return Cpu.tryParse(cpu);
    }


    @JSONProperty(ignore = true)
    public Optional<DataSize> getRequestedMemory(){
        return DataSize.tryParse(memory);
    }

}
