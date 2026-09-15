package com.eprint.admin.repository.qiniu.dao;

import com.eprint.admin.repository.qiniu.model.entity.QiniuTemplateType;
import com.eprint.admin.repository.qiniu.model.param.QiniuTemplateTypeCreateParam;
import com.eprint.admin.repository.qiniu.model.param.QiniuTemplateTypeDisableParam;
import com.eprint.admin.repository.qiniu.model.param.QiniuTemplateTypeEnableParam;
import com.eprint.admin.repository.qiniu.model.param.QiniuTemplateTypeModifyParam;
import com.eprint.admin.repository.qiniu.model.param.QiniuTemplateTypeRemoveParam;
import com.niko.boot.dao.BaseDao;
import jakarta.annotation.Resource;
import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository(value = "QiniuTemplateTypeDao")
public class QiniuTemplateTypeDao extends BaseDao {

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

    public int create(QiniuTemplateTypeCreateParam param) {
        return create("create", param);
    }

    public int remove(QiniuTemplateTypeRemoveParam param) {
        return getSqlSession().delete(getStatementName("remove"), param);
    }

    public int modify(QiniuTemplateTypeModifyParam param) {
        return modify("modify", param);
    }

    public int disable(QiniuTemplateTypeDisableParam param) {
        return modify("disable", param);
    }

    public int enable(QiniuTemplateTypeEnableParam param) {
        return modify("enable", param);
    }

    public List<QiniuTemplateType> queryEnabled() {
        return query("queryEnabled", null);
    }

    public QiniuTemplateType getByCode(String code) {
        return get("getByCode", code);
    }

    public int countTemplates(String id) {
        return count("countTemplates", id);
    }
}
