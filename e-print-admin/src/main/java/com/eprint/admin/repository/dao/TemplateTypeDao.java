package com.eprint.admin.repository.dao;

import com.eprint.admin.repository.model.entity.TemplateType;
import com.eprint.admin.repository.model.param.TemplateTypeCreateParam;
import com.eprint.admin.repository.model.param.TemplateTypeDisableParam;
import com.eprint.admin.repository.model.param.TemplateTypeEnableParam;
import com.eprint.admin.repository.model.param.TemplateTypeModifyParam;
import com.eprint.admin.repository.model.param.TemplateTypeRemoveParam;
import com.niko.boot.dao.BaseDao;
import jakarta.annotation.Resource;
import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository(value = "TemplateTypeDao")
public class TemplateTypeDao extends BaseDao {

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

    public int create(TemplateTypeCreateParam param) {
        return create("create", param);
    }

    public int remove(TemplateTypeRemoveParam param) {
        return getSqlSession().delete(getStatementName("remove"), param);
    }

    public int modify(TemplateTypeModifyParam param) {
        return modify("modify", param);
    }

    public int disable(TemplateTypeDisableParam param) {
        return modify("disable", param);
    }

    public int enable(TemplateTypeEnableParam param) {
        return modify("enable", param);
    }

    public List<TemplateType> queryEnabled() {
        return query("queryEnabled", null);
    }

    public TemplateType getByCode(String code) {
        return get("getByCode", code);
    }

    public int countTemplates(String id) {
        return count("countTemplates", id);
    }
}
