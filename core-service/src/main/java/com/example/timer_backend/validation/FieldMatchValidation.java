package com.example.timer_backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapperImpl;

public class FieldMatchValidation implements ConstraintValidator<FieldMatch, Object> {
    private String first;
    private String second;

    public void initialize(FieldMatch constraintAnnotation) {
        this.first = constraintAnnotation.first();
        this.second = constraintAnnotation.second();
    }

    public boolean isValid(Object value, ConstraintValidatorContext context) {
        Object fieldValue = new BeanWrapperImpl(value)
                .getPropertyValue(first);
        Object fieldMatchValue = new BeanWrapperImpl(value)
                .getPropertyValue(second);

        if (fieldValue == null || fieldMatchValue == null) {
            throw new RuntimeException("Some of object's values are null.");
        }

        return fieldValue.equals(fieldMatchValue);
    }
}
