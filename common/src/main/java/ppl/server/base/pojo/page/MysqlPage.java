package ppl.server.base.pojo.page;

public class MysqlPage implements DBPage {
    private final PageVo page;

    public MysqlPage(PageVo page) {
        this.page = page;
    }

    @Override
    public long getFirst() {
        return (page.getCurrentPage() - 1L) * page.getPageSize();
    }

    @Override
    public long getSecond() {
        return getFirst() + page.getPageSize();
    }
}
