package ppl.server.base.pojo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

/**
 * 下拉框数据
 * @param <K> 数据Key类型
 */
@Schema(description = "下拉框数据")
public class DropDown<K> {
    private final K id;
    private final String label;
    private boolean disabled;

    DropDown(K id, String label) {
        Objects.requireNonNull(id, "Id is required.");
        Objects.requireNonNull(label, "Label is required.");
        this.id = id;
        this.label = label;
    }

    /**
     * ID
     */
    @Schema(description = "ID")
    public K getId() {
        return id;
    }

    /**
     * 展示标签，带有样式
     */
    @Schema(description = "展示标签，带有样式")
    public String getLabel() {
        return label;
    }

    /**
     * 是否禁用
     */
    @Schema(description = "是否禁用")
    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public static <K> DropDown<K> create(K id, String label) {
        return new DropDown<>(id, label);
    }
}
