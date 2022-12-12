package com.sap.cloud.sdk.tutorial.utils;

import java.util.List;

import com.google.gson.Gson;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.sdk.cloudplatform.resilience.ResilienceConfiguration;
import com.sap.cloud.sdk.cloudplatform.resilience.ResilienceDecorator;
import com.sap.cloud.sdk.cloudplatform.resilience.ResilienceIsolationMode;
import com.sap.cloud.sdk.cloudplatform.resilience.ResilienceRuntimeException;
import com.sap.cloud.sdk.datamodel.odata.client.exception.ODataException;
import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultDestination;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpDestination;
import com.sap.cloud.sdk.datamodel.odata.helper.ModificationResponse;
import com.sap.cloud.sdk.datamodel.odata.helper.Order;
import com.sap.cloud.sdk.tutorial.vdm.namespaces.businesspartner.BusinessPartner;
import com.sap.cloud.sdk.tutorial.vdm.services.APIBUSINESSPARTNERService;
import com.sap.cloud.sdk.tutorial.vdm.services.DefaultAPIBUSINESSPARTNERService;

public class CreateBusinessPartnerCommand {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(GetBusinessPartnersCommand.class);

    private static final String CATEGORY_PERSON = "1";
    private final HttpDestination destination;
    private final BusinessPartner businessPartner;
    private static final String APIKEY_HEADER = "apikey";
    private static final String SANDBOX_APIKEY = "JIzPB8YwC3gFHFMfTmTks6yMxmQGKtuE";

    private final APIBUSINESSPARTNERService businessPartnerService;
    private final ResilienceConfiguration myResilienceConfig;

    public CreateBusinessPartnerCommand(HttpDestination destination, BusinessPartner businessPartner) {
        this(destination, businessPartner,
                new DefaultAPIBUSINESSPARTNERService().withServicePath("sap/opu/odata/sap/API_BUSINESS_PARTNER"));
    }

    public CreateBusinessPartnerCommand(HttpDestination destination, BusinessPartner businessPartner, APIBUSINESSPARTNERService service) {
        this.destination = destination;
        this.businessPartner = businessPartner;
        businessPartnerService = service;

        myResilienceConfig = ResilienceConfiguration.of(APIBUSINESSPARTNERService.class)
                .isolationMode(ResilienceIsolationMode.TENANT_AND_USER_OPTIONAL)
                .timeLimiterConfiguration(
                        ResilienceConfiguration.TimeLimiterConfiguration.of().timeoutDuration(Duration.ofMillis(10000)))
                .bulkheadConfiguration(ResilienceConfiguration.BulkheadConfiguration.of().maxConcurrentCalls(20));

    }

    public ModificationResponse<BusinessPartner> execute() {
        return ResilienceDecorator.executeSupplier(this::run, myResilienceConfig, e -> {
            logger.warn("Fallback called because of exception.", e);
            return null;
        });
    }

    private ModificationResponse<BusinessPartner> run() {
        try {
            return businessPartnerService.createBusinessPartner(businessPartner)
                    .withHeader(APIKEY_HEADER, SANDBOX_APIKEY)
                    .executeRequest(destination);
        } catch (ODataException e) {
            throw new ResilienceRuntimeException(e);
        }
    }
}
