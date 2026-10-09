package com.lmf.finpro.infrastructure.web.dto.privacy;

import com.lmf.finpro.application.privacy.AccountDeletionPreview;
import com.lmf.finpro.domain.model.HouseholdType;
import java.util.List;

public record AccountDeletionPreviewResponse(
        boolean canDelete,
        List<String> blockers,
        List<GroupResponse> deletedGroups,
        List<GroupResponse> leftGroups,
        int attachmentCount) {

    public record GroupResponse(
            Long id, String name, HouseholdType type, int accountsBroughtByYou) {}

    public static AccountDeletionPreviewResponse from(AccountDeletionPreview preview) {
        return new AccountDeletionPreviewResponse(
                preview.canDelete(),
                preview.blockers(),
                preview.deletedGroups().stream()
                        .map(AccountDeletionPreviewResponse::group)
                        .toList(),
                preview.leftGroups().stream().map(AccountDeletionPreviewResponse::group).toList(),
                preview.attachmentCount());
    }

    private static GroupResponse group(AccountDeletionPreview.GroupImpact impact) {
        return new GroupResponse(
                impact.householdId(), impact.name(), impact.type(), impact.accountsBroughtByYou());
    }
}
