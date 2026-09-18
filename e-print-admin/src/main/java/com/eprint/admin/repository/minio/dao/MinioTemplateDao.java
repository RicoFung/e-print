package com.eprint.admin.repository.minio.dao;

import com.eprint.admin.repository.minio.model.entity.MinioTemplate;
import com.eprint.admin.repository.minio.model.param.MinioTemplateCreateParam;
import com.eprint.admin.repository.minio.model.param.MinioTemplateDisableParam;
import com.eprint.admin.repository.minio.model.param.MinioTemplateEnableParam;
import com.eprint.admin.repository.minio.model.param.MinioTemplateModifyParam;
import com.eprint.admin.repository.minio.model.param.MinioTemplateRemoveParam;
import com.niko.boot.dao.BaseDao;
import jakarta.annotation.Resource;
import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository(value = "MinioTemplateDao")
public class MinioTemplateDao extends BaseDao {

    @Resource(name = "sqlSessionTemplateMybatis")
    private SqlSession sqlSession;

    @Override
    protected SqlSession getSqlSession() {
        return sqlSession;
    }

    @Override
    protected String getSqlNamespace() {
        return getClass().getName();
    }

    public int create(MinioTemplateCreateParam param) {
        return create("create", param);
    }

    public int remove(MinioTemplateRemoveParam param) {
        return getSqlSession().delete(getStatementName("remove"), param);
    }

    public int modify(MinioTemplateModifyParam param) {
        return modify("modify", param);
    }

    public int disable(MinioTemplateDisableParam param) {
        return modify("disable", param);
    }

    public int enable(MinioTemplateEnableParam param) {
        return modify("enable", param);
    }

    public MinioTemplate getByTemplateTypeIdAndCode(String templateTypeId, String templateCode) {
        return get("getByTemplateTypeIdAndCode", Map.of("templateTypeId", templateTypeId, "templateCode", templateCode));
    }
}
