package com.eprint.admin.module.template.model;

import com.eprint.admin.common.support.PageSupport;
import com.eprint.admin.module.template.model.request.TemplateCreateRequest;
import com.eprint.admin.module.template.model.request.TemplateDisableRequest;
import com.eprint.admin.module.template.model.request.TemplateEnableRequest;
import com.eprint.admin.module.template.model.request.TemplateModifyRequest;
import com.eprint.admin.module.template.model.request.TemplateQueryRequest;
import com.eprint.admin.module.template.model.request.TemplateRemoveRequest;
import com.eprint.admin.module.template.model.request.TemplateTypeCreateRequest;
import com.eprint.admin.module.template.model.request.TemplateTypeDisableRequest;
import com.eprint.admin.module.template.model.request.TemplateTypeEnableRequest;
import com.eprint.admin.module.template.model.request.TemplateTypeModifyRequest;
import com.eprint.admin.module.template.model.request.TemplateTypeQueryRequest;
import com.eprint.admin.module.template.model.request.TemplateTypeRemoveRequest;
import com.eprint.admin.repository.model.entity.Template;
import com.eprint.admin.repository.model.entity.TemplateType;
import com.eprint.admin.repository.model.param.TemplateCreateParam;
import com.eprint.admin.repository.model.param.TemplateDisableParam;
import com.eprint.admin.repository.model.param.TemplateEnableParam;
import com.eprint.admin.repository.model.param.TemplateModifyParam;
import com.eprint.admin.repository.model.param.TemplateQueryParam;
import com.eprint.admin.repository.model.param.TemplateRemoveParam;
import com.eprint.admin.repository.model.param.TemplateTypeCreateParam;
import com.eprint.admin.repository.model.param.TemplateTypeDisableParam;
import com.eprint.admin.repository.model.param.TemplateTypeEnableParam;
import com.eprint.admin.repository.model.param.TemplateTypeModifyParam;
import com.eprint.admin.repository.model.param.TemplateTypeQueryParam;
import com.eprint.admin.repository.model.param.TemplateTypeRemoveParam;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Stream;

@Mapper(imports = {PageSupport.class, StringUtils.class})
public interface ModelMapper {

    ModelMapper INSTANCE = Mappers.getMapper(ModelMapper.class);

    @Mapping(target = "templateTypeId", expression = "java(StringUtils.hasText(request.getTemplateTypeId()) ? request.getTemplateTypeId().trim() : null)")
    @Mapping(target = "templateCode", expression = "java(StringUtils.hasText(request.queryTemplateCode()) ? request.queryTemplateCode().trim() : null)")
    @Mapping(target = "page", expression = "java(request.page())")
    @Mapping(target = "pageSize", expression = "java(PageSupport.normalizePageSize(request.limit()))")
    TemplateQueryParam map(TemplateQueryRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "templateTypeId", source = "templateTypeId", qualifiedByName = "trim")
    @Mapping(target = "templateCode", source = "templateCode", qualifiedByName = "trim")
    @Mapping(target = "bucketName", source = "bucketName", qualifiedByName = "trim")
    TemplateCreateParam map(TemplateCreateRequest request);

    @Mapping(target = "templateTypeId", source = "templateTypeId", qualifiedByName = "trim")
    @Mapping(target = "templateCode", source = "templateCode", qualifiedByName = "trim")
    @Mapping(target = "bucketName", source = "bucketName", qualifiedByName = "trim")
    TemplateModifyParam map(TemplateModifyRequest request);

    @Mapping(target = "ids", expression = "java(ModelMapper.ids(request.getId(), request.getIds()))")
    TemplateRemoveParam map(TemplateRemoveRequest request);

    @Mapping(target = "ids", expression = "java(ModelMapper.ids(request.getId(), request.getIds()))")
    TemplateDisableParam map(TemplateDisableRequest request);

    @Mapping(target = "ids", expression = "java(ModelMapper.ids(request.getId(), request.getIds()))")
    TemplateEnableParam map(TemplateEnableRequest request);

    @Mapping(target = "content", ignore = true)
    TemplateModifyRequest map(Template template);

    @Mapping(target = "keyword", expression = "java(StringUtils.hasText(request.queryKeyword()) ? request.queryKeyword().trim() : null)")
    @Mapping(target = "page", expression = "java(request.page())")
    @Mapping(target = "pageSize", expression = "java(PageSupport.normalizePageSize(request.limit()))")
    TemplateTypeQueryParam map(TemplateTypeQueryRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", source = "code", qualifiedByName = "trim")
    @Mapping(target = "name", source = "name", qualifiedByName = "trim")
    TemplateTypeCreateParam map(TemplateTypeCreateRequest request);

    @Mapping(target = "code", source = "code", qualifiedByName = "trim")
    @Mapping(target = "name", source = "name", qualifiedByName = "trim")
    TemplateTypeModifyParam map(TemplateTypeModifyRequest request);

    @Mapping(target = "ids", expression = "java(ModelMapper.ids(request.getId(), request.getIds()))")
    TemplateTypeRemoveParam map(TemplateTypeRemoveRequest request);

    @Mapping(target = "ids", expression = "java(ModelMapper.ids(request.getId(), request.getIds()))")
    TemplateTypeDisableParam map(TemplateTypeDisableRequest request);

    @Mapping(target = "ids", expression = "java(ModelMapper.ids(request.getId(), request.getIds()))")
    TemplateTypeEnableParam map(TemplateTypeEnableRequest request);

    TemplateTypeModifyRequest map(TemplateType templateType);

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
