package com.eprint.admin.module.template.model;

import java.util.Arrays;
import java.util.List;

public enum TemplateType {
    SALES_RECEIPT("sales_receipt", "销售小票"),
    SALES_RECEIPT_ED("sales_receipt_ed", "销售小票-ed"),
    SALES_RECEIPT_ED2("sales_receipt_ed2", "销售小票-ed2"),
    SALES_RECEIPT_O2O("sales_receipt_o2o", "销售小票-o2o"),
    SHIPPING_LABEL("shipping_label", "物流面单"),
    SHIPPING_LABEL_O2O("shipping_label_o2o", "物流面单-o2o"),
    SHIPPING_LABEL_TRANSFER_OUT("shipping_label_transfer_out", "物流面单-横调出库"),
    SHIPPING_LABEL_RETURN_APPLY("shipping_label_return_apply", "物流面单-退货申请");

    public static final String DEFAULT_CODE = "01";
    public static final String DEFAULT_TYPE = "sales_receipt";

    private final String code;
    private final String label;

    TemplateType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static List<TemplateType> options() {
        return Arrays.asList(values());
    }

    public static boolean isValid(String code) {
        return options().stream().anyMatch(type -> type.code.equals(code));
    }
}
