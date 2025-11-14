package com.corems.common.exception.handler;

import com.corems.common.exception.model.Error;
import org.apache.commons.lang3.builder.CompareToBuilder;

import java.util.Comparator;

public class DefaultErrorComparator implements Comparator<Error> {

    @Override
    public int compare(Error e1, Error e2) {
        return new CompareToBuilder()
                .append(e1.getDescription(), e2.getDescription())
                .append(e1.getReasonCode(), e2.getReasonCode())
                .toComparison();
    }
}
