package com.bootdo.common.service;

import com.bootdo.common.domain.LogDO;
import com.bootdo.common.domain.PageDO;
import com.bootdo.common.utils.Query;

import java.util.List;

public interface LogService {
    public void save(LogDO logDO);

    public PageDO<LogDO> queryList(Query query);

    public int remove(Long id);

    public int batchRemove(Long[] ids);
}
