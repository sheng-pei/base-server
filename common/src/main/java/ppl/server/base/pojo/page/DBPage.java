package ppl.server.base.pojo.page;

public interface DBPage {
    long getFirst();
    long getSecond();

    static DBPage mysql(PageVo page) {
        return new MysqlPage(page);
    }

    static DBPage mysql(Integer currentPage, Integer pageSize) {
        PageVo vo = new PageVo();
        vo.setCurrentPage(currentPage);
        vo.setPageSize(pageSize);
        return mysql(vo);
    }
}
