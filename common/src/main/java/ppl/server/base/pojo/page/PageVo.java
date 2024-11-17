package ppl.server.base.pojo.page;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

/**
 * 分页参数
 */
@Schema(description = "分页参数")
public class PageVo {

    private static final int DEF_CURRENT_PAGE = 1;
    private static final int DEF_PAGE_SIZE = 20;

    /** 页号，从1开始 */
    @Schema(description = "页号，从1开始")
    @NotNull(message = "页号不能为NULL")
    @Min(value = 1, message = "页号必需从1开始")
    private Integer currentPage;

    /** 每页大小 */
    @Schema(description = "每页大小")
    @NotNull(message = "每页大小不能为NULL")
    @Positive(message = "每页大小必须大于0")
    private Integer pageSize;

    public PageVo() {
        this.currentPage = DEF_CURRENT_PAGE;
        this.pageSize = DEF_PAGE_SIZE;
    }

    public Integer getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(Integer currentPage) {
        this.currentPage = currentPage == null ? DEF_CURRENT_PAGE : currentPage;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize == null ? DEF_PAGE_SIZE : pageSize;
    }
}
