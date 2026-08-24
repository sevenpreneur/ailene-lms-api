package com.ailene.lms.common.pagination;

import java.util.List;

public record PagedResponse<T>(List<T> list, PageMeta metapaging) {
}
