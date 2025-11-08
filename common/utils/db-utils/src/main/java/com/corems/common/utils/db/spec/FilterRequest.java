package com.corems.common.utils.db.spec;

public record FilterRequest(String field, FilterOperation op, String value) {}

