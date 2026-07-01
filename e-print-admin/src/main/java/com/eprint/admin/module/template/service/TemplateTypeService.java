package com.eprint.admin.module.template.service;

import com.eprint.admin.common.model.page.PageResult;
import com.eprint.admin.module.template.model.ModelMapper;
import com.eprint.admin.module.template.model.request.TemplateTypeCreateRequest;
import com.eprint.admin.module.template.model.request.TemplateTypeDisableRequest;
import com.eprint.admin.module.template.model.request.TemplateTypeEnableRequest;
import com.eprint.admin.module.template.model.request.TemplateTypeModifyRequest;
import com.eprint.admin.module.template.model.request.TemplateTypeQueryRequest;
import com.eprint.admin.module.template.model.request.TemplateTypeRemoveRequest;
import com.eprint.admin.repository.dao.TemplateTypeDao;
import com.eprint.admin.repository.model.entity.TemplateType;
import com.eprint.admin.repository.model.param.TemplateTypeCreateParam;
import com.eprint.admin.repository.model.param.TemplateTypeModifyParam;
import com.eprint.admin.repository.model.param.TemplateTypeQueryParam;
import com.eprint.admin.repository.model.param.TemplateTypeRemoveParam;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class TemplateTypeService {

    private static final Integer STATUS_ENABLED = 1;

    private final TemplateTypeDao templateTypeDao;

    public TemplateTypeService(TemplateTypeDao templateTypeDao) {
        this.templateTypeDao = templateTypeDao;
    }

    public TemplateTypeCreateRequest createRequest() {
        return new TemplateTypeCreateRequest();
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public void create(TemplateTypeCreateRequest request) {
        TemplateTypeCreateParam param = ModelMapper.INSTANCE.map(request);
        if (templateTypeDao.getByCode(param.getCode()) != null) {
            throw new IllegalArgumentException("Template type code already exists");
        }
        templateTypeDao.create(param);
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public int remove(TemplateTypeRemoveRequest request) {
        TemplateTypeRemoveParam param = ModelMapper.INSTANCE.map(request);
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

    public TemplateTypeModifyRequest getModifyRequest(TemplateTypeModifyRequest request) {
        return ModelMapper.INSTANCE.map(get(request.getId()));
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public void modify(TemplateTypeModifyRequest request) {
        TemplateTypeModifyParam param = ModelMapper.INSTANCE.map(request);
        TemplateType existing = get(param.getId());
        TemplateType sameCode = templateTypeDao.getByCode(param.getCode());
        if (sameCode != null && !sameCode.getId().equals(existing.getId())) {
            throw new IllegalArgumentException("Template type code already exists");
        }
        templateTypeDao.modify(param);
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public int disable(TemplateTypeDisableRequest request) {
        var param = ModelMapper.INSTANCE.map(request);
        return param.getIds().length == 0 ? 0 : templateTypeDao.disable(param);
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public int enable(TemplateTypeEnableRequest request) {
        var param = ModelMapper.INSTANCE.map(request);
        return param.getIds().length == 0 ? 0 : templateTypeDao.enable(param);
    }

    public List<TemplateType> query(String keyword, Integer status) {
        TemplateTypeQueryParam param = new TemplateTypeQueryParam();
        param.setKeyword(StringUtils.hasText(keyword) ? keyword.trim() : null);
        param.setStatus(status);
        param.setPage(1);
        param.setPageSize(Integer.MAX_VALUE);
        return templateTypeDao.query(param);
    }

    public PageResult<TemplateType> query(TemplateTypeQueryRequest request) {
        TemplateTypeQueryParam param = ModelMapper.INSTANCE.map(request);
        return new PageResult<>(templateTypeDao.query(param), param.getPage(), param.getPageSize(), templateTypeDao.count(param));
    }

    public List<TemplateType> queryEnabled() {
        return templateTypeDao.queryEnabled();
    }

    public TemplateType get(String id) {
        TemplateType templateType = templateTypeDao.get(id);
        if (templateType == null) {
            throw new IllegalArgumentException("Template type not found");
        }
        return templateType;
    }

    public TemplateType getEnabled(String id) {
        TemplateType templateType = get(id);
        if (!STATUS_ENABLED.equals(templateType.getStatus())) {
            throw new IllegalArgumentException("Template type is disabled");
        }
        return templateType;
    }

}
