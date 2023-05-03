package com.bdea.grp2.lambda.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
@Setter
@ToString
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class TermId implements Serializable {
    private String term;
    private String fileName;
}
