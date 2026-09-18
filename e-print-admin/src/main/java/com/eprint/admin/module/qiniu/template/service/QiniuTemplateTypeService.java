package com.eprint.admin.module.qiniu.template.service;

import com.eprint.admin.common.model.page.PageResult;
import com.eprint.admin.module.qiniu.template.model.QiniuModelMapper;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateTypeCreateRequest;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateTypeDisableRequest;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateTypeEnableRequest;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateTypeModifyRequest;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateTypeQueryRequest;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateTypeRemoveRequest;
import com.eprint.admin.repository.qiniu.dao.QiniuTemplateTypeDao;
import com.eprint.admin.repository.qiniu.model.entity.QiniuTemplateType;
import com.eprint.admin.repository.qiniu.model.param.QiniuTemplateTypeCreateParam;
import com.eprint.admin.repository.qiniu.model.param.QiniuTemplateTypeModifyParam;
import com.eprint.admin.repository.qiniu.model.param.QiniuTemplateTypeQueryParam;
import com.eprint.admin.repository.qiniu.model.param.QiniuTemplateTypeRemoveParam;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class QiniuTemplateTypeService {

    private static final Integer STATUS_ENABLED = 1;

    private final QiniuTemplateTypeDao templateTypeDao;

    public QiniuTemplateTypeService(QiniuTemplateTypeDao templateTypeDao) {
        this.templateTypeDao = templateTypeDao;
    }

    public QiniuTemplateTypeCreateRequest createRequest() {
        return new QiniuTemplateTypeCreateRequest();
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public void create(QiniuTemplateTypeCreateRequest request) {
        QiniuTemplateTypeCreateParam param = QiniuModelMapper.INSTANCE.map(request);
        if (templateTypeDao.getByCode(param.getCode()) != null) {
            throw new IllegalArgumentException("Template type code already exists");
        }
        templateTypeDao.create(param);
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public int remove(QiniuTemplateTypeRemoveRequest request) {
        QiniuTemplateTypeRemoveParam param = QiniuModelMapper.INSTANCE.map(request);
        if (param.getIds().length == 0) {
            return 0;
        }
        for (String id : param.getIds()) {
            get(id);
            if (templateTypeDao.countTemplates(id) > 0) {
                throw new IllegalArgumentException("存在关联模板，禁止删除！");
            }
        }
        return templateTypeDao.remove(param);
    }

    public QiniuTemplateTypeModifyRequest getModifyRequest(QiniuTemplateTypeModifyRequest request) {
        return QiniuModelMapper.INSTANCE.map(get(request.getId()));
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public void modify(QiniuTemplateTypeModifyRequest request) {
        QiniuTemplateTypeModifyParam param = QiniuModelMapper.INSTANCE.map(request);
        QiniuTemplateType existing = get(param.getId());
        QiniuTemplateType sameCode = templateTypeDao.getByCode(param.getCode());
        if (sameCode != null && !sameCode.getId().equals(existing.getId())) {
            throw new IllegalArgumentException("Template type code already exists");
        }
        templateTypeDao.modify(param);
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public int disable(QiniuTemplateTypeDisableRequest request) {
        var param = QiniuModelMapper.INSTANCE.map(request);
        return param.getIds().length == 0 ? 0 : templateTypeDao.disable(param);
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public int enable(QiniuTemplateTypeEnableRequest request) {
        var param = QiniuModelMapper.INSTANCE.map(request);
        return param.getIds().length == 0 ? 0 : templateTypeDao.enable(param);
    }

    public List<QiniuTemplateType> query(String keyword, Integer status) {
        QiniuTemplateTypeQueryParam param = new QiniuTemplateTypeQueryParam();
        param.setKeyword(StringUtils.hasText(keyword) ? keyword.trim() : null);
        param.setStatus(status);
        param.setPage(1);
        param.setPageSize(Integer.MAX_VALUE);
        return templateTypeDao.query(param);
    }

    public PageResult<QiniuTemplateType> query(QiniuTemplateTypeQueryRequest request) {
        QiniuTemplateTypeQueryParam param = QiniuModelMapper.INSTANCE.map(request);
        return new PageResult<>(templateTypeDao.query(param), param.getPage(), param.getPageSize(), templateTypeDao.count(param));
    }

    public List<QiniuTemplateType> queryEnabled() {
        return templateTypeDao.queryEnabled();
    }

    public QiniuTemplateType get(String id) {
        QiniuTemplateType templateType = templateTypeDao.get(id);
        if (templateType == null) {
            throw new IllegalArgumentException("Template type not found");
        }
        return templateType;
    }

    public QiniuTemplateType getEnabled(String id) {
        QiniuTemplateType templateType = get(id);
        if (!STATUS_ENABLED.equals(templateType.getStatus())) {
            throw new IllegalArgumentException("Template type is disabled");
        }
        return templateType;
    }

}
