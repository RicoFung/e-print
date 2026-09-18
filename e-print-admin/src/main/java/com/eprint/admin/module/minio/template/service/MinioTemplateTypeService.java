package com.eprint.admin.module.minio.template.service;

import com.eprint.admin.common.model.page.PageResult;
import com.eprint.admin.module.minio.template.model.MinioModelMapper;
import com.eprint.admin.module.minio.template.model.request.MinioTemplateTypeCreateRequest;
import com.eprint.admin.module.minio.template.model.request.MinioTemplateTypeDisableRequest;
import com.eprint.admin.module.minio.template.model.request.MinioTemplateTypeEnableRequest;
import com.eprint.admin.module.minio.template.model.request.MinioTemplateTypeModifyRequest;
import com.eprint.admin.module.minio.template.model.request.MinioTemplateTypeQueryRequest;
import com.eprint.admin.module.minio.template.model.request.MinioTemplateTypeRemoveRequest;
import com.eprint.admin.repository.minio.dao.MinioTemplateTypeDao;
import com.eprint.admin.repository.minio.model.entity.MinioTemplateType;
import com.eprint.admin.repository.minio.model.param.MinioTemplateTypeCreateParam;
import com.eprint.admin.repository.minio.model.param.MinioTemplateTypeModifyParam;
import com.eprint.admin.repository.minio.model.param.MinioTemplateTypeQueryParam;
import com.eprint.admin.repository.minio.model.param.MinioTemplateTypeRemoveParam;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class MinioTemplateTypeService {

    private static final Integer STATUS_ENABLED = 1;

    private final MinioTemplateTypeDao templateTypeDao;

    public MinioTemplateTypeService(MinioTemplateTypeDao templateTypeDao) {
        this.templateTypeDao = templateTypeDao;
    }

    public MinioTemplateTypeCreateRequest createRequest() {
        return new MinioTemplateTypeCreateRequest();
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public void create(MinioTemplateTypeCreateRequest request) {
        MinioTemplateTypeCreateParam param = MinioModelMapper.INSTANCE.map(request);
        if (templateTypeDao.getByCode(param.getCode()) != null) {
            throw new IllegalArgumentException("Template type code already exists");
        }
        templateTypeDao.create(param);
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public int remove(MinioTemplateTypeRemoveRequest request) {
        MinioTemplateTypeRemoveParam param = MinioModelMapper.INSTANCE.map(request);
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

    public MinioTemplateTypeModifyRequest getModifyRequest(MinioTemplateTypeModifyRequest request) {
        return MinioModelMapper.INSTANCE.map(get(request.getId()));
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public void modify(MinioTemplateTypeModifyRequest request) {
        MinioTemplateTypeModifyParam param = MinioModelMapper.INSTANCE.map(request);
        MinioTemplateType existing = get(param.getId());
        MinioTemplateType sameCode = templateTypeDao.getByCode(param.getCode());
        if (sameCode != null && !sameCode.getId().equals(existing.getId())) {
            throw new IllegalArgumentException("Template type code already exists");
        }
        templateTypeDao.modify(param);
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public int disable(MinioTemplateTypeDisableRequest request) {
        var param = MinioModelMapper.INSTANCE.map(request);
        return param.getIds().length == 0 ? 0 : templateTypeDao.disable(param);
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public int enable(MinioTemplateTypeEnableRequest request) {
        var param = MinioModelMapper.INSTANCE.map(request);
        return param.getIds().length == 0 ? 0 : templateTypeDao.enable(param);
    }

    public List<MinioTemplateType> query(String keyword, Integer status) {
        MinioTemplateTypeQueryParam param = new MinioTemplateTypeQueryParam();
        param.setKeyword(StringUtils.hasText(keyword) ? keyword.trim() : null);
        param.setStatus(status);
        param.setPage(1);
        param.setPageSize(Integer.MAX_VALUE);
        return templateTypeDao.query(param);
    }

    public PageResult<MinioTemplateType> query(MinioTemplateTypeQueryRequest request) {
        MinioTemplateTypeQueryParam param = MinioModelMapper.INSTANCE.map(request);
        return new PageResult<>(templateTypeDao.query(param), param.getPage(), param.getPageSize(), templateTypeDao.count(param));
    }

    public List<MinioTemplateType> queryEnabled() {
        return templateTypeDao.queryEnabled();
    }

    public MinioTemplateType get(String id) {
        MinioTemplateType templateType = templateTypeDao.get(id);
        if (templateType == null) {
            throw new IllegalArgumentException("Template type not found");
        }
        return templateType;
    }

    public MinioTemplateType getEnabled(String id) {
        MinioTemplateType templateType = get(id);
        if (!STATUS_ENABLED.equals(templateType.getStatus())) {
            throw new IllegalArgumentException("Template type is disabled");
        }
        return templateType;
    }

}
