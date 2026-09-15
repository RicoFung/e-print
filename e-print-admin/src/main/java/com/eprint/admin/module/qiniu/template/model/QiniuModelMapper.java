package com.eprint.admin.module.qiniu.template.model;

import com.eprint.admin.common.support.PageSupport;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateCreateRequest;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateDisableRequest;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateEnableRequest;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateModifyRequest;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateQueryRequest;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateRemoveRequest;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateTypeCreateRequest;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateTypeDisableRequest;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateTypeEnableRequest;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateTypeModifyRequest;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateTypeQueryRequest;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateTypeRemoveRequest;
import com.eprint.admin.repository.qiniu.model.entity.QiniuTemplate;
import com.eprint.admin.repository.qiniu.model.entity.QiniuTemplateType;
import com.eprint.admin.repository.qiniu.model.param.QiniuTemplateCreateParam;
import com.eprint.admin.repository.qiniu.model.param.QiniuTemplateDisableParam;
import com.eprint.admin.repository.qiniu.model.param.QiniuTemplateEnableParam;
import com.eprint.admin.repository.qiniu.model.param.QiniuTemplateModifyParam;
import com.eprint.admin.repository.qiniu.model.param.QiniuTemplateQueryParam;
import com.eprint.admin.repository.qiniu.model.param.QiniuTemplateRemoveParam;
import com.eprint.admin.repository.qiniu.model.param.QiniuTemplateTypeCreateParam;
import com.eprint.admin.repository.qiniu.model.param.QiniuTemplateTypeDisableParam;
import com.eprint.admin.repository.qiniu.model.param.QiniuTemplateTypeEnableParam;
import com.eprint.admin.repository.qiniu.model.param.QiniuTemplateTypeModifyParam;
import com.eprint.admin.repository.qiniu.model.param.QiniuTemplateTypeQueryParam;
import com.eprint.admin.repository.qiniu.model.param.QiniuTemplateTypeRemoveParam;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Stream;

@Mapper(imports = {PageSupport.class, StringUtils.class})
public interface QiniuModelMapper {

    QiniuModelMapper INSTANCE = Mappers.getMapper(QiniuModelMapper.class);

    @Mapping(target = "templateTypeId", expression = "java(StringUtils.hasText(request.getTemplateTypeId()) ? request.getTemplateTypeId().trim() : null)")
    @Mapping(target = "templateCode", expression = "java(StringUtils.hasText(request.queryTemplateCode()) ? request.queryTemplateCode().trim() : null)")
    @Mapping(target = "page", expression = "java(request.page())")
    @Mapping(target = "pageSize", expression = "java(PageSupport.normalizePageSize(request.limit()))")
    QiniuTemplateQueryParam map(QiniuTemplateQueryRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "templateTypeId", source = "templateTypeId", qualifiedByName = "trim")
    @Mapping(target = "templateCode", source = "templateCode", qualifiedByName = "trim")
    @Mapping(target = "bucketName", source = "bucketName", qualifiedByName = "trim")
    QiniuTemplateCreateParam map(QiniuTemplateCreateRequest request);

    @Mapping(target = "templateTypeId", source = "templateTypeId", qualifiedByName = "trim")
    @Mapping(target = "templateCode", source = "templateCode", qualifiedByName = "trim")
    @Mapping(target = "bucketName", source = "bucketName", qualifiedByName = "trim")
    QiniuTemplateModifyParam map(QiniuTemplateModifyRequest request);

    @Mapping(target = "ids", expression = "java(QiniuModelMapper.ids(request.getId(), request.getIds()))")
    QiniuTemplateRemoveParam map(QiniuTemplateRemoveRequest request);

    @Mapping(target = "ids", expression = "java(QiniuModelMapper.ids(request.getId(), request.getIds()))")
    QiniuTemplateDisableParam map(QiniuTemplateDisableRequest request);

    @Mapping(target = "ids", expression = "java(QiniuModelMapper.ids(request.getId(), request.getIds()))")
    QiniuTemplateEnableParam map(QiniuTemplateEnableRequest request);

    @Mapping(target = "content", ignore = true)
    QiniuTemplateModifyRequest map(QiniuTemplate template);

    @Mapping(target = "keyword", expression = "java(StringUtils.hasText(request.queryKeyword()) ? request.queryKeyword().trim() : null)")
    @Mapping(target = "page", expression = "java(request.page())")
    @Mapping(target = "pageSize", expression = "java(PageSupport.normalizePageSize(request.limit()))")
    QiniuTemplateTypeQueryParam map(QiniuTemplateTypeQueryRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", source = "code", qualifiedByName = "trim")
    @Mapping(target = "name", source = "name", qualifiedByName = "trim")
    QiniuTemplateTypeCreateParam map(QiniuTemplateTypeCreateRequest request);

    @Mapping(target = "code", source = "code", qualifiedByName = "trim")
    @Mapping(target = "name", source = "name", qualifiedByName = "trim")
    QiniuTemplateTypeModifyParam map(QiniuTemplateTypeModifyRequest request);

    @Mapping(target = "ids", expression = "java(QiniuModelMapper.ids(request.getId(), request.getIds()))")
    QiniuTemplateTypeRemoveParam map(QiniuTemplateTypeRemoveRequest request);

    @Mapping(target = "ids", expression = "java(QiniuModelMapper.ids(request.getId(), request.getIds()))")
    QiniuTemplateTypeDisableParam map(QiniuTemplateTypeDisableRequest request);

    @Mapping(target = "ids", expression = "java(QiniuModelMapper.ids(request.getId(), request.getIds()))")
    QiniuTemplateTypeEnableParam map(QiniuTemplateTypeEnableRequest request);

    QiniuTemplateTypeModifyRequest map(QiniuTemplateType templateType);

    static String[] ids(String id, List<String> ids) {
        return Stream.concat(Stream.of(id), ids == null ? Stream.empty() : ids.stream())
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toArray(String[]::new);
    }

    @Named("trim")
    static String trim(String value) {
        return value == null ? null : value.trim();
    }

}
