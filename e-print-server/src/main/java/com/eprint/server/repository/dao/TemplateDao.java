package com.eprint.server.repository.dao;

import com.eprint.server.repository.model.param.TemplateGetByCodeParam;
import com.eprint.server.repository.model.result.TemplateResult;
import com.niko.boot.dao.BaseDao;
import jakarta.annotation.Resource;
import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Repository;

@Repository(value = "TemplateDao")
public class TemplateDao extends BaseDao {

    @Resource
    private SqlSession sqlSession;

    @Override
    protected SqlSession getSqlSession() {
        return sqlSession;
    }

    @Override
    protected String getSqlNamespace() {
        return getClass().getName();
    }

    public TemplateResult getByTemplateCode(TemplateGetByCodeParam param) {
        return get("getByTemplateCode", param);
    }
}
