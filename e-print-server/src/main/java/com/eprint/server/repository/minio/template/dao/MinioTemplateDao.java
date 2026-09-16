package com.eprint.server.repository.minio.template.dao;

import com.eprint.server.repository.minio.template.model.param.MinioTemplateGetByCodeParam;
import com.eprint.server.repository.minio.template.model.result.MinioTemplateResult;
import com.niko.boot.dao.BaseDao;
import jakarta.annotation.Resource;
import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Repository;

@Repository(value = "MinioTemplateDao")
public class MinioTemplateDao extends BaseDao {

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

    public MinioTemplateResult getByTemplateCode(MinioTemplateGetByCodeParam param) {
        return get("getByTemplateCode", param);
    }
}
