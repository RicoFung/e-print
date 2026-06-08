package com.eprint.admin.repository.dao;

import com.eprint.admin.repository.model.entity.Template;
import com.eprint.admin.repository.model.param.TemplateQueryParam;
import jakarta.annotation.Resource;
import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository(value = "TemplateDao")
public class TemplateDao {

    private static final String NAMESPACE = TemplateDao.class.getName();

    @Resource(name = "sqlSessionTemplateMybatis")
    private SqlSession sqlSession;

    public List<Template> list(TemplateQueryParam param) {
        return sqlSession.selectList(statement("list"), param);
    }

    public int count(TemplateQueryParam param) {
        Integer count = sqlSession.selectOne(statement("count"), param);
        return count == null ? 0 : count;
    }

    public Template getById(String id) {
        return sqlSession.selectOne(statement("getById"), id);
    }

    public Template getByTemplateCode(String templateCode) {
        return sqlSession.selectOne(statement("getByTemplateCode"), templateCode);
    }

    public int insert(Template template) {
        return sqlSession.insert(statement("insert"), template);
    }

    public int update(Template template) {
        return sqlSession.update(statement("update"), template);
    }

    public int disable(String id) {
        return sqlSession.update(statement("disable"), id);
    }

    public int enable(String id) {
        return sqlSession.update(statement("enable"), id);
    }

    public int updateStatusByIds(List<String> ids, Integer status) {
        return sqlSession.update(statement("updateStatusByIds"), Map.of("ids", ids, "status", status));
    }

    public int deleteById(String id) {
        return sqlSession.delete(statement("deleteById"), id);
    }

    public int deleteByIds(List<String> ids) {
        return sqlSession.delete(statement("deleteByIds"), ids);
    }

    private String statement(String id) {
        return NAMESPACE + "." + id;
    }
}
