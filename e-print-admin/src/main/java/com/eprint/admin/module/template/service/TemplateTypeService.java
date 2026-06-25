package com.eprint.admin.module.template.service;

import com.eprint.admin.module.template.model.PageResult;
import com.eprint.admin.module.template.model.request.TemplateTypeCreateRequest;
import com.eprint.admin.module.template.model.request.TemplateTypeModifyRequest;
import com.eprint.admin.module.template.model.request.TemplateTypeQueryRequest;
import com.eprint.admin.repository.dao.TemplateTypeDao;
import com.eprint.admin.repository.model.entity.TemplateType;
import com.eprint.admin.repository.model.param.TemplateTypeQueryParam;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

@Service
public class TemplateTypeService {

    private static final Integer STATUS_ENABLED = 1;
    private static final Integer STATUS_DISABLED = 0;
    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "code", "T.CODE",
            "name", "T.NAME",
            "sortNo", "T.SORT_NO",
            "status", "T.STATUS"
    );

    private final TemplateTypeDao templateTypeDao;

    public TemplateTypeService(TemplateTypeDao templateTypeDao) {
        this.templateTypeDao = templateTypeDao;
    }

    public List<TemplateType> query(String keyword, Integer status) {
        TemplateTypeQueryParam param = new TemplateTypeQueryParam();
        param.setKeyword(StringUtils.hasText(keyword) ? keyword.trim() : null);
        param.setStatus(status);
        param.setPage(1);
        param.setPageSize(Integer.MAX_VALUE);
        param.setOrderBy(toOrderBy(null));
        return templateTypeDao.query(param);
    }

    public PageResult<TemplateType> query(TemplateTypeQueryRequest request) {
        int normalizedPageSize = normalizePageSize(request.limit());
        int normalizedPage = request.page();
        TemplateTypeQueryParam param = new TemplateTypeQueryParam();
        param.setKeyword(StringUtils.hasText(request.queryKeyword()) ? request.queryKeyword().trim() : null);
        param.setStatus(request.getStatus());
        param.setOrderBy(toOrderBy(request.getSort()));

        int total = templateTypeDao.count(param);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / normalizedPageSize);
        if (totalPages > 0 && normalizedPage > totalPages) {
            normalizedPage = totalPages;
        }

        param.setPage(normalizedPage);
        param.setPageSize(normalizedPageSize);
        List<TemplateType> records = templateTypeDao.query(param);
        return new PageResult<>(records, normalizedPage, normalizedPageSize, total);
    }

    public List<TemplateType> queryEnabled() {
        return templateTypeDao.queryEnabled();
    }

    public TemplateType getRequired(String id) {
        TemplateType templateType = templateTypeDao.get(id);
        if (templateType == null) {
            throw new IllegalArgumentException("Template type not found");
        }
        return templateType;
    }

    public TemplateType getEnabledRequired(String id) {
        TemplateType templateType = getRequired(id);
        if (!STATUS_ENABLED.equals(templateType.getStatus())) {
            throw new IllegalArgumentException("Template type is disabled");
        }
        return templateType;
    }

    public TemplateTypeCreateRequest createRequest() {
        TemplateTypeCreateRequest request = new TemplateTypeCreateRequest();
        request.setStatus(STATUS_ENABLED);
        request.setSortNo(0);
        return request;
    }

    public TemplateTypeModifyRequest getModifyRequest(TemplateTypeModifyRequest request) {
        return toModifyRequest(getRequired(request.getId()));
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public void create(TemplateTypeCreateRequest request) {
        normalize(request);
        if (templateTypeDao.getByCode(request.getCode()) != null) {
            throw new IllegalArgumentException("Template type code already exists");
        }
        templateTypeDao.create(toEntity(request));
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public void modify(TemplateTypeModifyRequest request) {
        TemplateType existing = getRequired(request.getId());
        normalize(request);
        TemplateType sameCode = templateTypeDao.getByCode(request.getCode());
        if (sameCode != null && !sameCode.getId().equals(existing.getId())) {
            throw new IllegalArgumentException("Template type code already exists");
        }
        TemplateType templateType = toEntity(request);
        templateType.setId(request.getId());
        templateTypeDao.modify(templateType);
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public void enable(String id) {
        getRequired(id);
        templateTypeDao.updateStatus(id, STATUS_ENABLED);
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public void disable(String id) {
        getRequired(id);
        templateTypeDao.updateStatus(id, STATUS_DISABLED);
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public int disable(List<String> ids) {
        return updateStatus(ids, STATUS_DISABLED);
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public int enable(List<String> ids) {
        return updateStatus(ids, STATUS_ENABLED);
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public void remove(String id) {
        getRequired(id);
        if (templateTypeDao.countTemplates(id) > 0) {
            throw new IllegalArgumentException("存在关联模板，禁止删除！");
        }
        templateTypeDao.remove(new String[]{id});
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public int remove(List<String> ids) {
        List<String> normalizedIds = normalizeIds(ids);
        if (normalizedIds.isEmpty()) {
            return 0;
        }
        for (String id : normalizedIds) {
            getRequired(id);
            if (templateTypeDao.countTemplates(id) > 0) {
                throw new IllegalArgumentException("存在关联模板，禁止删除！");
            }
        }
        return templateTypeDao.remove(normalizedIds.toArray(String[]::new));
    }

    private int updateStatus(List<String> ids, Integer status) {
        List<String> normalizedIds = normalizeIds(ids);
        if (normalizedIds.isEmpty()) {
            return 0;
        }
        return templateTypeDao.updateStatusByIds(normalizedIds.toArray(String[]::new), status);
    }

    private List<String> normalizeIds(List<String> ids) {
        return ids == null ? List.of() : ids.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private String toOrderBy(String sort) {
        List<String> clauses = new ArrayList<>();
        LinkedHashSet<String> sortedFields = new LinkedHashSet<>();

        if (StringUtils.hasText(sort)) {
            for (String token : sort.split(",")) {
                if (clauses.size() >= 4) {
                    break;
                }
                String normalizedToken = token == null ? "" : token.trim();
                int separatorIndex = normalizedToken.lastIndexOf('.');
                if (separatorIndex <= 0 || separatorIndex >= normalizedToken.length() - 1) {
                    continue;
                }

                String field = normalizedToken.substring(0, separatorIndex);
                String direction = normalizedToken.substring(separatorIndex + 1).toUpperCase(Locale.ROOT);
                String column = SORT_COLUMNS.get(field);
                if (column == null || (!"ASC".equals(direction) && !"DESC".equals(direction)) || !sortedFields.add(field)) {
                    continue;
                }
                clauses.add(column + " " + direction);
            }
        }

        if (!sortedFields.contains("sortNo")) {
            clauses.add("T.SORT_NO ASC");
        }
        clauses.add("T.ID ASC");

        StringJoiner orderBy = new StringJoiner(", ");
        clauses.forEach(orderBy::add);
        return orderBy.toString();
    }

    private void normalize(TemplateTypeCreateRequest request) {
        request.setCode(request.getCode().trim());
        request.setName(request.getName().trim());
        if (request.getStatus() == null) {
            request.setStatus(STATUS_ENABLED);
        }
        if (!STATUS_ENABLED.equals(request.getStatus()) && !STATUS_DISABLED.equals(request.getStatus())) {
            throw new IllegalArgumentException("Invalid template type status");
        }
        if (request.getSortNo() == null) {
            request.setSortNo(0);
        }
    }

    private void normalize(TemplateTypeModifyRequest request) {
        request.setCode(request.getCode().trim());
        request.setName(request.getName().trim());
        if (request.getStatus() == null) {
            request.setStatus(STATUS_ENABLED);
        }
        if (!STATUS_ENABLED.equals(request.getStatus()) && !STATUS_DISABLED.equals(request.getStatus())) {
            throw new IllegalArgumentException("Invalid template type status");
        }
        if (request.getSortNo() == null) {
            request.setSortNo(0);
        }
    }

    private TemplateType toEntity(TemplateTypeCreateRequest request) {
        TemplateType templateType = new TemplateType();
        templateType.setCode(request.getCode());
        templateType.setName(request.getName());
        templateType.setStatus(request.getStatus());
        templateType.setSortNo(request.getSortNo());
        return templateType;
    }

    private TemplateType toEntity(TemplateTypeModifyRequest request) {
        TemplateType templateType = new TemplateType();
        templateType.setId(request.getId());
        templateType.setCode(request.getCode());
        templateType.setName(request.getName());
        templateType.setStatus(request.getStatus());
        templateType.setSortNo(request.getSortNo());
        return templateType;
    }

    private TemplateTypeModifyRequest toModifyRequest(TemplateType templateType) {
        TemplateTypeModifyRequest request = new TemplateTypeModifyRequest();
        request.setId(templateType.getId());
        request.setCode(templateType.getCode());
        request.setName(templateType.getName());
        request.setStatus(templateType.getStatus());
        request.setSortNo(templateType.getSortNo());
        return request;
    }

    private static int normalizePageSize(Integer pageSize) {
        if (pageSize == null) {
            return 10;
        }
        if (pageSize < 5) {
            return 5;
        }
        return Math.min(pageSize, 100);
    }
}
