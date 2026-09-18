package com.eprint.admin.repository.minio.dao;

import com.eprint.admin.repository.minio.model.entity.MinioTemplateType;
import com.eprint.admin.repository.minio.model.param.MinioTemplateTypeCreateParam;
import com.eprint.admin.repository.minio.model.param.MinioTemplateTypeDisableParam;
import com.eprint.admin.repository.minio.model.param.MinioTemplateTypeEnableParam;
import com.eprint.admin.repository.minio.model.param.MinioTemplateTypeModifyParam;
import com.eprint.admin.repository.minio.model.param.MinioTemplateTypeRemoveParam;
import com.niko.boot.dao.BaseDao;
import jakarta.annotation.Resource;
import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository(value = "MinioTemplateTypeDao")
public class MinioTemplateTypeDao extends BaseDao {

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

    public int create(MinioTemplateTypeCreateParam param) {
        return create("create", param);
    }

    public int remove(MinioTemplateTypeRemoveParam param) {
        return getSqlSession().delete(getStatementName("remove"), param);
    }

    public int modify(MinioTemplateTypeModifyParam param) {
        return modify("modify", param);
    }

    public int disable(MinioTemplateTypeDisableParam param) {
        return modify("disable", param);
    }

    public int enable(MinioTemplateTypeEnableParam param) {
        return modify("enable", param);
    }

    public List<MinioTemplateType> queryEnabled() {
        return query("queryEnabled", null);
    }

    public MinioTemplateType getByCode(String code) {
        return get("getByCode", code);
    }

    public int countTemplates(String id) {
        return count("countTemplates", id);
    }
}
