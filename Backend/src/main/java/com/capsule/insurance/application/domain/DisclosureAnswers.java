package com.capsule.insurance.application.domain;

public record DisclosureAnswers(
        Boolean diagnosedCancer,
        Boolean underCancerExamination,
        Boolean recentHospitalization
) {

    public boolean complete() {
        return diagnosedCancer != null
                && underCancerExamination != null
                && recentHospitalization != null;
    }
}
