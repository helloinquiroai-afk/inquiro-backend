package com.inquiro.business;

import java.util.List;

public record BusinessBoundaries(
        List<String> supported,
        List<String> notSupported,
        List<String> requiresHuman
) {

    public BusinessBoundaries {

        supported =
                supported == null
                        ? List.of()
                        : List.copyOf(supported);

        notSupported =
                notSupported == null
                        ? List.of()
                        : List.copyOf(notSupported);

        requiresHuman =
                requiresHuman == null
                        ? List.of()
                        : List.copyOf(requiresHuman);
    }
}
