package ppl.server.base.pojo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 带附加数据的下拉框数据
 * @param <K> 数据Key类型
 * @param <T> 附加数据类型
 */
public class AttachmentDropDown<K, T> extends DropDown<K> {

    private T attachment;

    AttachmentDropDown(K id, String label) {
        super(id, label);
    }

    /**
     * 下拉框附加数据
     */
    @Schema(description = "下拉框附加数据")
    public T getAttachment() {
        return attachment;
    }

    public void setAttachment(T attachment) {
        this.attachment = attachment;
    }

    public static <K, T> AttachmentDropDown<K, T> createAttachmentDropDown(K id, String label) {
        return new AttachmentDropDown<>(id, label);
    }
}
