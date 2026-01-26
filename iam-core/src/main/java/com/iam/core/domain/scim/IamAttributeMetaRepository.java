package com.iam.core.domain.scim;

import com.iam.core.domain.common.enums.AttributeCategory;
import com.iam.core.domain.common.enums.AttributeTargetDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IamAttributeMetaRepository extends JpaRepository<IamAttributeMeta, String> {
    List<IamAttributeMeta> findByTargetDomain(AttributeTargetDomain targetDomain);

    List<IamAttributeMeta> findByTargetDomainAndCategory(AttributeTargetDomain targetDomain,
            AttributeCategory category);
}
