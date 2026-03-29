package com.rental.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelDTO {
    private Integer modelId;
    private String modelName;
    private Integer brandId;
    private String brandName;
}
