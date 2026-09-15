package com.eprint.admin.repository.qiniu.dao;

import com.eprint.admin.repository.qiniu.model.entity.QiniuTemplate;
import com.eprint.admin.repository.qiniu.model.param.QiniuTemplateCreateParam;
import com.eprint.admin.repository.qiniu.model.param.QiniuTemplateDisableParam;
import com.eprint.admin.repository.qiniu.model.param.QiniuTemplateEnableParam;
import com.eprint.admin.repository.qiniu.model.param.QiniuTemplateModifyParam;
import com.eprint.admin.repository.qiniu.model.param.QiniuTemplateRemoveParam;
import com.niko.boot.dao.BaseDao;
import jakarta.annotation.Resource;
import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository(value = "QiniuTemplateDao")
public class QiniuTemplateDao extends BaseDao {

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

    public int create(QiniuTemplateCreateParam param) {
        return create("create", param);
    }

    public int remove(QiniuTemplateRemoveParam param) {
        return getSqlSession().delete(getStatementName("remove"), param);
    }

    public int modify(QiniuTemplateModifyParam param) {
        return modify("modify", param);
    }

    public int disable(QiniuTemplateDisableParam param) {
        return modify("disable", param);
    }

    public int enable(QiniuTemplateEnableParam param) {
        return modify("enable", param);
    }

    public QiniuTemplate getByTemplateTypeIdAndCode(String templateTypeId, String templateCode) {
        return get("getByTemplateTypeIdAndCode", Map.of("templateTypeId", templateTypeId, "templateCode", templateCode));
    }

    public QiniuTemplate getByBucketNameAndObjectName(String bucketName, String objectName) {
        return get("getByBucketNameAndObjectName", Map.of("bucketName", bucketName, "objectName", objectName));
    }
}
