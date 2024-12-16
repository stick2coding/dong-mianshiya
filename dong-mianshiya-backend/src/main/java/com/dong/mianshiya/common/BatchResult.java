package com.dong.mianshiya.common;

import java.util.List;

/**
 * 通用的批量操作返回
 */
public class BatchResult {

    private int totalCount;

    private int successCount;

    private int failCount;

    private List<String> failedReasonList;

}
