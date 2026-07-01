package com.eprint.admin.repository.dao;

import com.eprint.admin.repository.model.entity.Template;
import com.eprint.admin.repository.model.param.TemplateCreateParam;
import com.eprint.admin.repository.model.param.TemplateDisableParam;
import com.eprint.admin.repository.model.param.TemplateEnableParam;
import com.eprint.admin.repository.model.param.TemplateModifyParam;
import com.eprint.admin.repository.model.param.TemplateRemoveParam;
import com.niko.boot.dao.BaseDao;
import jakarta.annotation.Resource;
import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository(value = "TemplateDao")
public class TemplateDao extends BaseDao {

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

    public int create(TemplateCreateParam param) {
        return create("create", param);
    }

    public int remove(TemplateRemoveParam param) {
        return getSqlSession().delete(getStatementName("remove"), param);
    }

    public int modify(TemplateModifyParam param) {
        return modify("modify", param);
    }

    public int disable(TemplateDisableParam param) {
        return modify("disable", param);
    }

    public int enable(TemplateEnableParam param) {
        return modify("enable", param);
    }

    public Template getByTemplateTypeIdAndCode(String templateTypeId, String templateCode) {
        return get("getByTemplateTypeIdAndCode", Map.of("templateTypeId", templateTypeId, "templateCode", templateCode));
    }
}
