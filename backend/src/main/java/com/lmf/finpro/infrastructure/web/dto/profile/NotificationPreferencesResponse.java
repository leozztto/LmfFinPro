package com.lmf.finpro.infrastructure.web.dto.profile;

public record NotificationPreferencesResponse(
        boolean billsEnabled, int billDaysBefore, boolean budgetsEnabled, boolean dasEnabled) {}
