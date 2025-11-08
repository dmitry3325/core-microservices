package com.corems.common.utils.db.utils;

import java.util.List;
import java.util.Optional;

public record QueryParams(
        Optional<Integer> page,
        Optional<Integer> pageSize,
        Optional<String> search,
        Optional<String> sort,
        Optional<List<String>> filters
) {}
