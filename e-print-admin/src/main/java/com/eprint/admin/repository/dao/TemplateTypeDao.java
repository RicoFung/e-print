package com.eprint.admin.repository.dao;

import com.eprint.admin.repository.model.entity.TemplateType;
import com.niko.boot.dao.BaseDao;
import jakarta.annotation.Resource;
import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

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

    public List<TemplateType> queryEnabled() {
        return query("queryEnabled", null);
    }

    public TemplateType getByCode(String code) {
        return get("getByCode", code);
    }

    public int updateStatus(String id, Integer status) {
        TemplateType templateType = new TemplateType();
        templateType.setId(id);
        templateType.setStatus(status);
        return modify("updateStatus", templateType);
    }

    public int updateStatusByIds(String[] ids, Integer status) {
        return modify("updateStatusByIds", Map.of("ids", ids, "status", status));
    }

    public int countTemplates(String id) {
        return count("countTemplates", id);
    }
}
