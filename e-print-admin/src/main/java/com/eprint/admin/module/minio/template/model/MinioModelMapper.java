package com.eprint.admin.module.minio.template.model;

import com.eprint.admin.common.support.PageSupport;
import com.eprint.admin.module.minio.template.model.request.MinioTemplateCreateRequest;
import com.eprint.admin.module.minio.template.model.request.MinioTemplateDisableRequest;
import com.eprint.admin.module.minio.template.model.request.MinioTemplateEnableRequest;
import com.eprint.admin.module.minio.template.model.request.MinioTemplateModifyRequest;
import com.eprint.admin.module.minio.template.model.request.MinioTemplateQueryRequest;
import com.eprint.admin.module.minio.template.model.request.MinioTemplateRemoveRequest;
import com.eprint.admin.module.minio.template.model.request.MinioTemplateTypeCreateRequest;
import com.eprint.admin.module.minio.template.model.request.MinioTemplateTypeDisableRequest;
import com.eprint.admin.module.minio.template.model.request.MinioTemplateTypeEnableRequest;
import com.eprint.admin.module.minio.template.model.request.MinioTemplateTypeModifyRequest;
import com.eprint.admin.module.minio.template.model.request.MinioTemplateTypeQueryRequest;
import com.eprint.admin.module.minio.template.model.request.MinioTemplateTypeRemoveRequest;
import com.eprint.admin.repository.minio.model.entity.MinioTemplate;
import com.eprint.admin.repository.minio.model.entity.MinioTemplateType;
import com.eprint.admin.repository.minio.model.param.MinioTemplateCreateParam;
import com.eprint.admin.repository.minio.model.param.MinioTemplateDisableParam;
import com.eprint.admin.repository.minio.model.param.MinioTemplateEnableParam;
import com.eprint.admin.repository.minio.model.param.MinioTemplateModifyParam;
import com.eprint.admin.repository.minio.model.param.MinioTemplateQueryParam;
import com.eprint.admin.repository.minio.model.param.MinioTemplateRemoveParam;
import com.eprint.admin.repository.minio.model.param.MinioTemplateTypeCreateParam;
import com.eprint.admin.repository.minio.model.param.MinioTemplateTypeDisableParam;
import com.eprint.admin.repository.minio.model.param.MinioTemplateTypeEnableParam;
import com.eprint.admin.repository.minio.model.param.MinioTemplateTypeModifyParam;
import com.eprint.admin.repository.minio.model.param.MinioTemplateTypeQueryParam;
import com.eprint.admin.repository.minio.model.param.MinioTemplateTypeRemoveParam;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Stream;

@Mapper(imports = {PageSupport.class, StringUtils.class})
public interface MinioModelMapper {

    MinioModelMapper INSTANCE = Mappers.getMapper(MinioModelMapper.class);

    @Mapping(target = "templateTypeId", expression = "java(StringUtils.hasText(request.getTemplateTypeId()) ? request.getTemplateTypeId().trim() : null)")
    @Mapping(target = "templateCode", expression = "java(StringUtils.hasText(request.queryTemplateCode()) ? request.queryTemplateCode().trim() : null)")
    @Mapping(target = "page", expression = "java(request.page())")
    @Mapping(target = "pageSize", expression = "java(PageSupport.normalizePageSize(request.limit()))")
    MinioTemplateQueryParam map(MinioTemplateQueryRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "templateTypeId", source = "templateTypeId", qualifiedByName = "trim")
    @Mapping(target = "templateCode", source = "templateCode", qualifiedByName = "trim")
    @Mapping(target = "bucketName", source = "bucketName", qualifiedByName = "trim")
    MinioTemplateCreateParam map(MinioTemplateCreateRequest request);

    @Mapping(target = "templateTypeId", source = "templateTypeId", qualifiedByName = "trim")
    @Mapping(target = "templateCode", source = "templateCode", qualifiedByName = "trim")
    @Mapping(target = "bucketName", source = "bucketName", qualifiedByName = "trim")
    MinioTemplateModifyParam map(MinioTemplateModifyRequest request);

    @Mapping(target = "ids", expression = "java(MinioModelMapper.ids(request.getId(), request.getIds()))")
    MinioTemplateRemoveParam map(MinioTemplateRemoveRequest request);

    @Mapping(target = "ids", expression = "java(MinioModelMapper.ids(request.getId(), request.getIds()))")
    MinioTemplateDisableParam map(MinioTemplateDisableRequest request);

    @Mapping(target = "ids", expression = "java(MinioModelMapper.ids(request.getId(), request.getIds()))")
    MinioTemplateEnableParam map(MinioTemplateEnableRequest request);

    @Mapping(target = "content", ignore = true)
    MinioTemplateModifyRequest map(MinioTemplate template);

    @Mapping(target = "keyword", expression = "java(StringUtils.hasText(request.queryKeyword()) ? request.queryKeyword().trim() : null)")
    @Mapping(target = "page", expression = "java(request.page())")
    @Mapping(target = "pageSize", expression = "java(PageSupport.normalizePageSize(request.limit()))")
    MinioTemplateTypeQueryParam map(MinioTemplateTypeQueryRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", source = "code", qualifiedByName = "trim")
    @Mapping(target = "name", source = "name", qualifiedByName = "trim")
    MinioTemplateTypeCreateParam map(MinioTemplateTypeCreateRequest request);

    @Mapping(target = "code", source = "code", qualifiedByName = "trim")
    @Mapping(target = "name", source = "name", qualifiedByName = "trim")
    MinioTemplateTypeModifyParam map(MinioTemplateTypeModifyRequest request);

    @Mapping(target = "ids", expression = "java(MinioModelMapper.ids(request.getId(), request.getIds()))")
    MinioTemplateTypeRemoveParam map(MinioTemplateTypeRemoveRequest request);

    @Mapping(target = "ids", expression = "java(MinioModelMapper.ids(request.getId(), request.getIds()))")
    MinioTemplateTypeDisableParam map(MinioTemplateTypeDisableRequest request);

    @Mapping(target = "ids", expression = "java(MinioModelMapper.ids(request.getId(), request.getIds()))")
    MinioTemplateTypeEnableParam map(MinioTemplateTypeEnableRequest request);

    MinioTemplateTypeModifyRequest map(MinioTemplateType templateType);

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
