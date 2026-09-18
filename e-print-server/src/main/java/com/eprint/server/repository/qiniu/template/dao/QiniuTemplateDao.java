package com.eprint.server.repository.qiniu.template.dao;

import com.eprint.server.repository.qiniu.template.model.param.QiniuTemplateGetByCodeParam;
import com.eprint.server.repository.qiniu.template.model.result.QiniuTemplateResult;
import com.niko.boot.dao.BaseDao;
import jakarta.annotation.Resource;
import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Repository;

@Repository(value = "QiniuTemplateDao")
public class QiniuTemplateDao extends BaseDao {

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

    public QiniuTemplateResult getByTemplateCode(QiniuTemplateGetByCodeParam param) {
        return get("getByTemplateCode", param);
    }
}
