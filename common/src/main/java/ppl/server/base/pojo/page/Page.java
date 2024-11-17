package ppl.server.base.pojo.page;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Collections;
import java.util.List;

/**
 * 分页数据
 * @param <T> 记录类型
 */
@Schema(description = "分页数据")
public class Page<T> {
    private final long currentPage;
    private final int pageSize;
    private final long totalCount;
    private final List<T> data;

    private Page(long currentPage, int pageSize) {
        if (pageSize < 1) {
            throw new IllegalArgumentException("PageSize must be positive.");
        }

        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalCount = 0;
        this.data = Collections.emptyList();
    }

    private Page(Page<T> source, long totalCount, List<T> data) {
        this.currentPage = source.getCurrentPage();
        this.pageSize = source.getPageSize();
        this.totalCount = totalCount;
        this.data = data;
    }

    /**
     * 页号
     */
    @Schema(description = "页号")
    public long getCurrentPage() {
        return currentPage;
    }

    /**
     * 页大小
     */
    @Schema(description = "页大小")
    public int getPageSize() {
        return pageSize;
    }

    /**
     * 记录总数
     */
    @Schema(description = "记录总数")
    public long getTotalCount() {
        return totalCount;
    }

    /**
     * 页总数
     */
    @Schema(description = "页总数")
    public long getTotalPage() {
        return totalCount / pageSize + (totalCount % pageSize != 0 ? 1 : 0);
    }

    /**
     * 记录
     */
    @Schema(description = "记录")
    public List<T> getData() {
        return data;
    }

    public Builder<T> builder() {
        return new Builder<>(this);
    }

    public static <T> Builder<T> builder(int currentPage, int pageSize) {
        return new Page<T>(currentPage, pageSize).builder();
    }

    public static <T> Builder<T> builder(PageVo vo) {
        return builder(vo.getCurrentPage(), vo.getPageSize());
    }

    public static final class Builder<T> {
        private final Page<T> source;
        private long totalCount;
        private List<T> data;

        private Builder(Page<T> source) {
            this.source = source;
        }

        public Builder<T> totalCount(long totalCount) {
            this.totalCount = totalCount;
            return this;
        }

        public Builder<T> data(List<T> data) {
            this.data = data;
            return this;
        }

        public Page<T> build() {
            return new Page<>(source, totalCount, data);
        }
    }

}
