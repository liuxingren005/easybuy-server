package org.maven.request;

import lombok.Data;

import java.util.List;

/**
 * 退款请求体
 */
@Data
public class Refund {

    /**
     * 退款明细列表（为空=全额退款）
     */
    private List<RefundDetail> items;

    /**
     * 退款原因（可选）
     */
    private String reason;
}
