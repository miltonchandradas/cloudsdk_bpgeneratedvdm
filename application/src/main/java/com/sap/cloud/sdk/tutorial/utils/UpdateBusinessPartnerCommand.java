package com.sap.cloud.sdk.tutorial.utils;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.sdk.cloudplatform.resilience.ResilienceConfiguration;
import com.sap.cloud.sdk.cloudplatform.resilience.ResilienceDecorator;
import com.sap.cloud.sdk.cloudplatform.resilience.ResilienceIsolationMode;
import com.sap.cloud.sdk.cloudplatform.resilience.ResilienceRuntimeException;
import com.sap.cloud.sdk.datamodel.odata.client.exception.ODataException;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpDestination;
import com.sap.cloud.sdk.tutorial.vdm.namespaces.businesspartner.BusinessPartner;
import com.sap.cloud.sdk.tutorial.vdm.services.APIBUSINESSPARTNERService;
import com.sap.cloud.sdk.tutorial.vdm.services.DefaultAPIBUSINESSPARTNERService;

public class UpdateBusinessPartnerCommand {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(GetBusinessPartnersCommand.class);

    private final HttpDestination destination;
    private final String id;
    private final BusinessPartner businessPartner;
    private static final String APIKEY_HEADER = "apikey";
    private static final String SANDBOX_APIKEY = "<YOUR APIKEY GOES HERE>";

    private final APIBUSINESSPARTNERService businessPartnerService;
    private final ResilienceConfiguration myResilienceConfig;

    public UpdateBusinessPartnerCommand(HttpDestination destination, String id, BusinessPartner businessPartner) {
        this(destination, id, businessPartner,
                new DefaultAPIBUSINESSPARTNERService().withServicePath("sap/opu/odata/sap/API_BUSINESS_PARTNER"));
    }

    public UpdateBusinessPartnerCommand(HttpDestination destination, String id, BusinessPartner businessPartner, APIBUSINESSPARTNERService service) {
        this.destination = destination;
        this.id = id;
        this.businessPartner = businessPartner;
        businessPartnerService = service;

        myResilienceConfig = ResilienceConfiguration.of(APIBUSINESSPARTNERService.class)
                .isolationMode(ResilienceIsolationMode.TENANT_AND_USER_OPTIONAL)
                .timeLimiterConfiguration(
                        ResilienceConfiguration.TimeLimiterConfiguration.of().timeoutDuration(Duration.ofMillis(10000)))
                .bulkheadConfiguration(ResilienceConfiguration.BulkheadConfiguration.of().maxConcurrentCalls(20));

    }

    public String execute() {
        return ResilienceDecorator.executeSupplier(this::run, myResilienceConfig, e -> {
            logger.warn("Fallback called because of exception.", e);
            return "Business Partner update failed...";
        });
    }

    private String run() {
        try {
            BusinessPartner bp = businessPartnerService.getBusinessPartnerByKey(id)
                    .select(BusinessPartner.BUSINESS_PARTNER, BusinessPartner.LAST_NAME, BusinessPartner.FIRST_NAME,
                            BusinessPartner.IS_MALE, BusinessPartner.IS_FEMALE, BusinessPartner.CREATION_DATE)
                    .withHeader(APIKEY_HEADER, SANDBOX_APIKEY)
                    .executeRequest(destination);

            bp.setFirstName(businessPartner.FIRST_NAME.toString());
            businessPartnerService.updateBusinessPartner(bp);

            return "Business Partner updated succeeded...";

        } catch (ODataException e) {
            throw new ResilienceRuntimeException(e);
        }
    }
}
